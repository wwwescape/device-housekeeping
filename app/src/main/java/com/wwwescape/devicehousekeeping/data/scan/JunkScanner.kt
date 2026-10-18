package com.wwwescape.devicehousekeeping.data.scan

import android.content.Context
import android.os.Build
import com.wwwescape.devicehousekeeping.data.settings.CleaningSettings
import com.wwwescape.devicehousekeeping.data.storage.ScanVolume
import com.wwwescape.devicehousekeeping.data.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.nio.file.Files

sealed interface ScanEvent {
    data class Progress(val progress: ScanProgress) : ScanEvent
    data class Finished(val result: ScanResult) : ScanEvent
}

/** Size, file count and media-file count of a directory tree. */
internal data class TreeStats(val bytes: Long, val files: Int, val mediaFiles: Int)

/**
 * Walks every reachable storage volume once, applying [JunkRules] to each file and the
 * folder-level rules (thumbnail caches, app caches, leftovers) to each directory. Symbolic links
 * are never followed, so the walk can't escape a volume or loop.
 */
class JunkScanner(private val context: Context) {

    fun scan(settings: CleaningSettings): Flow<ScanEvent> = flow {
        val startedAt = System.currentTimeMillis()
        val volumes = StorageRepository.scanVolumes(context, settings.scanRemovableStorage)
        val walker = Walker(
            packages = PackageIndex(context),
            settings = settings,
            ownPackage = context.packageName,
            now = startedAt,
            emitProgress = { emit(ScanEvent.Progress(it)) },
        )
        for (volume in volumes) walker.walkVolume(volume)

        val result = ScanResult(
            items = walker.items.sortedByDescending { it.sizeBytes },
            volumes = volumes.map { ScanVolumeSummary(it.root.absolutePath, it.label, it.isPrimary) },
            filesScanned = walker.filesScanned,
            durationMillis = System.currentTimeMillis() - startedAt,
            finishedAt = System.currentTimeMillis(),
            residualsChecked = walker.packages.canSeeAllPackages,
            appCachesReachable = Build.VERSION.SDK_INT < Build.VERSION_CODES.R,
        )
        emit(ScanEvent.Finished(result))
    }.flowOn(Dispatchers.IO)

    private class Walker(
        val packages: PackageIndex,
        private val settings: CleaningSettings,
        private val ownPackage: String,
        private val now: Long,
        private val emitProgress: suspend (ScanProgress) -> Unit,
    ) {
        val items = mutableListOf<JunkItem>()
        var filesScanned = 0
            private set

        private val foundByCategory = mutableMapOf<JunkCategory, Long>()
        private var foundBytes = 0L
        private var lastEmit = 0L
        private val excluded = settings.excludedPaths.map { it.trimEnd('/') }

        suspend fun walkVolume(volume: ScanVolume) {
            val rootPath = volume.root.absolutePath.trimEnd('/')
            val stack = ArrayDeque<Pair<File, Int>>()
            stack.addLast(volume.root to 0)
            while (stack.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val (dir, depth) = stack.removeLast()
                val children = dir.listFiles() ?: continue
                for (child in children) {
                    if (isSymlink(child)) continue
                    val path = child.absolutePath
                    if (isExcluded(path)) continue
                    val relative = path.removePrefix(rootPath).trimStart('/')
                    if (child.isDirectory) {
                        when {
                            depth == 0 && child.name.equals("Android", ignoreCase = true) -> scanAndroidDir(child, rootPath)
                            JunkRules.isThumbnailDir(child.name) -> addTree(child, JunkCategory.THUMBNAILS, rootPath)
                            depth + 1 < MAX_DEPTH -> stack.addLast(child to depth + 1)
                        }
                    } else {
                        filesScanned++
                        val topLevel = if (depth == 0) null else relative.substringBefore('/').lowercase()
                        classifyFile(child, topLevel, rootPath)
                    }
                    maybeEmit(path)
                }
            }
        }

        /** `Android/` holds per-app folders. Only two things are ever offered from it: the
         * `cache` folder of an installed app (where reachable) and the whole folder of an app
         * that's no longer installed. Nothing else in it is walked. */
        private fun scanAndroidDir(androidDir: File, rootPath: String) {
            for (area in androidDir.listFiles().orEmpty()) {
                if (isSymlink(area) || !area.isDirectory) continue
                val areaName = area.name.lowercase()
                if (areaName != "data" && areaName != "media" && areaName != "obb") continue
                for (appDir in area.listFiles().orEmpty()) {
                    if (isSymlink(appDir) || !appDir.isDirectory || isExcluded(appDir.absolutePath)) continue
                    val pkg = appDir.name
                    if (pkg == ownPackage || !JunkRules.looksLikePackageName(pkg)) continue
                    val installed = packages.isInstalled(pkg)
                    when {
                        !installed && packages.canSeeAllPackages -> {
                            val stats = treeStats(appDir)
                            if (stats.bytes > 0) {
                                add(appDir, JunkCategory.RESIDUALS, rootPath, stats, ItemDetail.Residual(pkg, stats.mediaFiles))
                            }
                        }
                        installed && areaName == "data" -> {
                            val cache = File(appDir, "cache")
                            if (cache.isDirectory && !isSymlink(cache) && !isExcluded(cache.absolutePath)) {
                                val stats = treeStats(cache)
                                if (stats.bytes > 0) {
                                    add(cache, JunkCategory.APP_CACHES, rootPath, stats, ItemDetail.AppCache(pkg, packages.label(pkg)))
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun classifyFile(file: File, topLevel: String?, rootPath: String) {
            val size = file.length()
            val verdict = JunkRules.classifyFile(
                name = file.name,
                topLevelDir = topLevel,
                sizeBytes = size,
                lastModified = file.lastModified(),
                now = now,
                minAgeMillis = settings.minFileAge.millis,
            ) ?: return
            when (verdict) {
                is FileVerdict.Junk -> add(file, verdict.category, rootPath, TreeStats(size, 1, 0), verdict.detail)
                FileVerdict.ApkCandidate -> {
                    val apk = packages.readArchive(file.absolutePath)
                    val category = if (apk?.isAlreadyInstalled() == true) JunkCategory.INSTALLED_APKS else JunkCategory.OTHER_APKS
                    add(file, category, rootPath, TreeStats(size, 1, 0), apk ?: ItemDetail.UnreadableApk)
                }
            }
        }

        private fun addTree(dir: File, category: JunkCategory, rootPath: String) {
            val stats = treeStats(dir)
            if (stats.bytes > 0) add(dir, category, rootPath, stats, null)
        }

        private fun add(file: File, category: JunkCategory, rootPath: String, stats: TreeStats, detail: ItemDetail?) {
            val relative = file.absolutePath.removePrefix(rootPath)
            if (file.isDirectory && JunkRules.isProtectedDir(relative)) return
            items += JunkItem(
                path = file.absolutePath,
                category = category,
                sizeBytes = stats.bytes,
                lastModified = file.lastModified(),
                isDirectory = file.isDirectory,
                fileCount = stats.files,
                detail = detail,
                volumeRoot = rootPath,
            )
            foundBytes += stats.bytes
            foundByCategory[category] = (foundByCategory[category] ?: 0L) + stats.bytes
        }

        private suspend fun maybeEmit(path: String) {
            val t = System.currentTimeMillis()
            if (t - lastEmit < PROGRESS_INTERVAL_MILLIS) return
            lastEmit = t
            emitProgress(
                ScanProgress(
                    currentPath = path,
                    filesScanned = filesScanned,
                    foundBytes = foundBytes,
                    foundByCategory = foundByCategory.toMap(),
                    startedAt = now,
                ),
            )
        }

        private fun isExcluded(path: String): Boolean =
            excluded.any { path == it || path.startsWith("$it/") }
    }

    companion object {
        private const val MAX_DEPTH = 48
        private const val PROGRESS_INTERVAL_MILLIS = 80L
    }
}

internal fun isSymlink(file: File): Boolean = runCatching { Files.isSymbolicLink(file.toPath()) }.getOrDefault(true)

/** Walks a directory tree without following links. */
internal fun treeStats(dir: File): TreeStats {
    var bytes = 0L
    var files = 0
    var media = 0
    val stack = ArrayDeque<Pair<File, Int>>()
    stack.addLast(dir to 0)
    while (stack.isNotEmpty()) {
        val (current, depth) = stack.removeLast()
        for (child in current.listFiles().orEmpty()) {
            if (isSymlink(child)) continue
            if (child.isDirectory) {
                if (depth < 64) stack.addLast(child to depth + 1)
            } else {
                bytes += child.length()
                files++
                if (JunkRules.isMediaFile(child.name)) media++
            }
        }
    }
    return TreeStats(bytes, files, media)
}

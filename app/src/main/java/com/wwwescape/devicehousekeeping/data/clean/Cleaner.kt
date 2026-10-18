package com.wwwescape.devicehousekeeping.data.clean

import android.content.Context
import android.media.MediaScannerConnection
import com.wwwescape.devicehousekeeping.data.scan.ItemDetail
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.data.scan.JunkItem
import com.wwwescape.devicehousekeeping.data.scan.PackageIndex
import com.wwwescape.devicehousekeeping.data.scan.isAlreadyInstalled
import com.wwwescape.devicehousekeeping.data.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class CleanProgress(
    val itemsDone: Int = 0,
    val itemsTotal: Int = 0,
    val bytesTotal: Long = 0,
    val freedBytes: Long = 0,
    val currentPath: String = "",
) {
    val fraction: Float get() = if (bytesTotal > 0) (freedBytes.toFloat() / bytesTotal).coerceIn(0f, 1f) else if (itemsTotal > 0) itemsDone.toFloat() / itemsTotal else 0f
}

data class CleanReport(
    /** Sum of the sizes of the files actually deleted. */
    val freedBytes: Long,
    /** Change in free space measured by the file system before vs after — the real gain. */
    val measuredFreedBytes: Long,
    val freeBefore: Long,
    val freeAfter: Long,
    val deletedItems: Int,
    val deletedFiles: Int,
    val failedFiles: Int,
    val skipped: Map<SkipReason, Int>,
    val freedByCategory: Map<JunkCategory, Long>,
    val durationMillis: Long,
    val finishedAt: Long,
)

sealed interface CleanEvent {
    data class Progress(val progress: CleanProgress) : CleanEvent
    data class Finished(val report: CleanReport, val deletedPaths: Set<String>) : CleanEvent
}

class Cleaner(private val context: Context) {

    fun clean(items: List<JunkItem>, excludedPaths: Set<String>): Flow<CleanEvent> = flow {
        val startedAt = System.currentTimeMillis()
        val roots = items.map { it.volumeRoot }.distinct()
        val rootFiles = roots.map(::File)
        val packages = PackageIndex(context)
        val deleter = SafeDeleter(roots, excludedPaths) { item -> stillJunk(item, packages) }
        val freeBefore = StorageRepository.freeBytes(rootFiles)

        val bytesTotal = items.sumOf { it.sizeBytes }
        var freed = 0L
        var deletedItems = 0
        var deletedFiles = 0
        var failedFiles = 0
        val skipped = mutableMapOf<SkipReason, Int>()
        val byCategory = mutableMapOf<JunkCategory, Long>()
        val deletedPaths = mutableSetOf<String>()
        val scanTargets = mutableListOf<String>()
        var lastEmit = 0L

        // Smallest first: the progress ring moves steadily instead of stalling on one big folder
        // at the start.
        items.sortedBy { it.sizeBytes }.forEachIndexed { index, item ->
            currentCoroutineContext().ensureActive()
            val outcome = deleter.delete(item)
            freed += outcome.freedBytes
            deletedFiles += outcome.deletedFiles
            failedFiles += outcome.failedFiles
            outcome.skipped?.let { skipped[it] = (skipped[it] ?: 0) + 1 }
            if (outcome.skipped == null && outcome.deletedFiles > 0) {
                deletedItems++
                deletedPaths += item.path
                byCategory[item.category] = (byCategory[item.category] ?: 0L) + outcome.freedBytes
                if (!item.isDirectory) scanTargets += item.path
            }
            val now = System.currentTimeMillis()
            if (now - lastEmit > PROGRESS_INTERVAL_MILLIS || index == items.lastIndex) {
                lastEmit = now
                emit(CleanEvent.Progress(CleanProgress(index + 1, items.size, bytesTotal, freed, item.path)))
            }
        }

        // Keeps MediaStore in step on Android 10 and older; on 11+ the file system already
        // reports deletions to it, and scanning a missing path is a harmless no-op.
        if (scanTargets.isNotEmpty()) {
            runCatching { MediaScannerConnection.scanFile(context, scanTargets.toTypedArray(), null, null) }
        }
        // Let the file system settle (deferred block release on f2fs/FUSE) before measuring.
        delay(SETTLE_MILLIS)
        val freeAfter = StorageRepository.freeBytes(rootFiles)

        emit(
            CleanEvent.Finished(
                CleanReport(
                    freedBytes = freed,
                    measuredFreedBytes = (freeAfter - freeBefore).coerceAtLeast(0),
                    freeBefore = freeBefore,
                    freeAfter = freeAfter,
                    deletedItems = deletedItems,
                    deletedFiles = deletedFiles,
                    failedFiles = failedFiles,
                    skipped = skipped,
                    freedByCategory = byCategory,
                    durationMillis = System.currentTimeMillis() - startedAt,
                    finishedAt = System.currentTimeMillis(),
                ),
                deletedPaths,
            ),
        )
    }.flowOn(Dispatchers.IO)

    /** Re-checks each item's category rule against the device as it is right now. */
    private fun stillJunk(item: JunkItem, packages: PackageIndex): Boolean = when (val detail = item.detail) {
        is ItemDetail.Residual -> packages.canSeeAllPackages && !packages.isInstalled(detail.packageName)
        is ItemDetail.AppCache -> File(item.path).name == "cache"
        is ItemDetail.Apk -> when (item.category) {
            JunkCategory.INSTALLED_APKS -> packages.readArchive(item.path)?.isAlreadyInstalled() == true
            else -> true
        }
        else -> true
    }

    private companion object {
        const val PROGRESS_INTERVAL_MILLIS = 50L
        const val SETTLE_MILLIS = 600L
    }
}

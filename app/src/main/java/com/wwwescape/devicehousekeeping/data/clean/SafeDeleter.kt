package com.wwwescape.devicehousekeeping.data.clean

import com.wwwescape.devicehousekeeping.data.scan.JunkItem
import com.wwwescape.devicehousekeeping.data.scan.JunkRules
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/** Why the deleter refused an item. Refusing is always the safe outcome. */
enum class SkipReason {
    /** The path isn't inside one of the scanned volumes (or tries to escape one). */
    OUTSIDE_VOLUME,

    /** The path is a volume root or one of the user's top-level folders. */
    PROTECTED,

    /** The item itself is a symbolic link. */
    SYMLINK,

    /** The file was modified, resized or replaced after the scan. */
    CHANGED,

    /** The item no longer exists. */
    MISSING,

    /** The user excluded the path (or a parent) after scanning. */
    EXCLUDED,

    /** The item's rule no longer holds (e.g. its app has been reinstalled meanwhile). */
    NO_LONGER_JUNK,
}

data class DeleteOutcome(
    val freedBytes: Long = 0,
    val deletedFiles: Int = 0,
    val failedFiles: Int = 0,
    val skipped: SkipReason? = null,
)

/**
 * The only code in the app that deletes anything. Before touching an item it re-checks, from
 * scratch, everything that made the scanner offer it — so a stale or tampered scan result can at
 * worst be refused, never cause damage:
 *
 * 1. The canonical path lies strictly inside one of [volumeRoots] (no `..`, no link escapes).
 * 2. It isn't a volume root or a protected top-level folder.
 * 3. It isn't itself a symbolic link, and isn't under an excluded path.
 * 4. A file still has exactly the size and modification time the scan saw.
 * 5. [stillJunk] confirms the category rule still holds.
 *
 * Directories are removed bottom-up without following links: a link inside a junk folder is
 * unlinked, its target is never visited.
 */
class SafeDeleter(
    volumeRoots: List<String>,
    excludedPaths: Set<String>,
    private val stillJunk: (JunkItem) -> Boolean = { true },
) {
    private val roots: List<String> = volumeRoots.map { File(it).canonicalPath.normalized().trimEnd('/') }
    private val excluded: List<String> = excludedPaths.map { it.normalized().trimEnd('/') }

    fun delete(item: JunkItem): DeleteOutcome {
        val file = File(item.path)
        check(item)?.let { return DeleteOutcome(skipped = it) }
        return if (file.isDirectory) deleteTree(file) else deleteFile(file)
    }

    /** Returns why [item] must be skipped, or null when it may be deleted. */
    fun check(item: JunkItem): SkipReason? {
        val file = File(item.path)
        val path = item.path.normalized()
        if (!file.isAbsolute || path.split('/').any { it == ".." || it == "." }) return SkipReason.OUTSIDE_VOLUME
        if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) return SkipReason.MISSING
        if (Files.isSymbolicLink(file.toPath())) return SkipReason.SYMLINK

        val canonical = runCatching { file.canonicalPath.normalized() }.getOrNull() ?: return SkipReason.OUTSIDE_VOLUME
        val root = roots.firstOrNull { canonical.startsWith("$it/") } ?: return SkipReason.OUTSIDE_VOLUME
        if (JunkRules.isProtectedDir(canonical.removePrefix(root))) return SkipReason.PROTECTED
        if (excluded.any { path == it || path.startsWith("$it/") }) return SkipReason.EXCLUDED

        if (item.isDirectory) {
            if (!file.isDirectory) return SkipReason.CHANGED
        } else {
            if (!file.isFile || file.length() != item.sizeBytes || file.lastModified() != item.lastModified) {
                return SkipReason.CHANGED
            }
        }
        if (!stillJunk(item)) return SkipReason.NO_LONGER_JUNK
        return null
    }

    private fun deleteFile(file: File): DeleteOutcome {
        val size = file.length()
        return if (file.delete()) DeleteOutcome(freedBytes = size, deletedFiles = 1) else DeleteOutcome(failedFiles = 1)
    }

    private fun deleteTree(dir: File): DeleteOutcome {
        var freed = 0L
        var deleted = 0
        var failed = 0

        fun visit(current: File, depth: Int) {
            for (child in current.listFiles().orEmpty()) {
                val path = child.toPath()
                when {
                    Files.isSymbolicLink(path) -> {
                        // Removes the link only, never what it points to.
                        if (runCatching { Files.deleteIfExists(path) }.getOrDefault(false)) deleted++ else failed++
                    }
                    child.isDirectory -> {
                        if (depth < MAX_DEPTH) visit(child, depth + 1)
                        if (!child.delete()) failed++
                    }
                    else -> {
                        val size = child.length()
                        if (child.delete()) {
                            freed += size
                            deleted++
                        } else {
                            failed++
                        }
                    }
                }
            }
        }

        visit(dir, 0)
        if (!dir.delete()) failed++
        return DeleteOutcome(freedBytes = freed, deletedFiles = deleted, failedFiles = failed)
    }

    private companion object {
        const val MAX_DEPTH = 64

        /** Android paths already use '/'; this only matters when the unit tests run on Windows. */
        fun String.normalized(): String = if (File.separatorChar == '\\') replace('\\', '/') else this
    }
}

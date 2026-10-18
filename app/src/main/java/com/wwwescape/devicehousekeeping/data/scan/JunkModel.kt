package com.wwwescape.devicehousekeeping.data.scan

/** How much the user needs to think before an item is removed. */
enum class Safety {
    /** Regenerated or useless by definition — preselected after a scan. */
    SAFE,

    /** Very likely junk, but it could hold something the user still wants — never preselected,
     * the user has to opt in item by item or per category. */
    REVIEW,
}

/**
 * Everything the scanner can offer. Deliberately short: each category is only here because it
 * frees real space *and* there's a rule that can tell junk from data with certainty (SAFE) or
 * near-certainty the user confirms (REVIEW). Nothing is guessed from file names alone inside the
 * user's own photo, music, video or document folders.
 */
enum class JunkCategory(val safety: Safety) {
    /** `Android/data/<package>/cache` of installed apps — Android 10 and older only, where those
     * folders are still reachable. Apps must cope with their cache vanishing at any time. */
    APP_CACHES(Safety.SAFE),

    /** `.thumbnails` folders — preview images galleries regenerate on demand. */
    THUMBNAILS(Safety.SAFE),

    /** Abandoned temporary files and partial downloads, older than the configured minimum age. */
    TEMP_FILES(Safety.SAFE),

    /** Log files, crash dumps and heap dumps outside the user's own folders. */
    LOGS(Safety.SAFE),

    /** Files other operating systems leave behind (Thumbs.db, .DS_Store, desktop.ini, ._ files)
     * when storage is used with a PC or Mac. Android never reads them. */
    FOREIGN_FILES(Safety.SAFE),

    /** Installer files (APKs) for apps that are already installed at the same or a newer
     * version. */
    INSTALLED_APKS(Safety.SAFE),

    /** Installer files for apps that aren't installed, are installed at an older version, or
     * that can't be read. */
    OTHER_APKS(Safety.REVIEW),

    /** Folders left behind by apps that have been uninstalled. */
    RESIDUALS(Safety.REVIEW),

    /** Files you already deleted that are waiting out the system's 30-day recycle bin. */
    TRASH(Safety.REVIEW),
}

/** Extra facts about an item, shown in its detail row so the user can see *why* it was offered. */
sealed interface ItemDetail {
    data class Apk(
        val packageName: String,
        val label: String?,
        val apkVersionName: String?,
        val apkVersionCode: Long,
        val installedVersionName: String?,
        val installedVersionCode: Long?,
    ) : ItemDetail

    /** An installer file Android couldn't parse — usually an incomplete download. */
    data object UnreadableApk : ItemDetail

    data class Residual(val packageName: String, val mediaFiles: Int) : ItemDetail

    data class AppCache(val packageName: String, val label: String?) : ItemDetail

    /** [expiresAt] is when the system itself would empty this item, if known. */
    data class Trashed(val expiresAt: Long?) : ItemDetail

    /** A MediaStore "pending" file whose writer never finished. */
    data class Pending(val expiresAt: Long?) : ItemDetail
}

data class JunkItem(
    /** Absolute path; also the item's identity for selection. */
    val path: String,
    val category: JunkCategory,
    val sizeBytes: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    /** Files inside a directory item (1 for plain files). */
    val fileCount: Int = 1,
    val detail: ItemDetail? = null,
    /** Root of the storage volume the item lives on. */
    val volumeRoot: String,
) {
    val name: String get() = path.substringAfterLast('/')
}

data class ScanVolumeSummary(val root: String, val label: String, val isPrimary: Boolean)

data class ScanResult(
    val items: List<JunkItem>,
    val volumes: List<ScanVolumeSummary>,
    val filesScanned: Int,
    val durationMillis: Long,
    val finishedAt: Long,
    /** False when package visibility couldn't be confirmed, so leftover-folder detection was
     * skipped rather than risk calling an installed app's folder a leftover. */
    val residualsChecked: Boolean,
    /** False on Android 11+, where other apps' caches can only be cleared through the system. */
    val appCachesReachable: Boolean,
) {
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }

    fun itemsIn(category: JunkCategory): List<JunkItem> = items.filter { it.category == category }

    /** Categories that have at least one item, in [JunkCategory] declaration order. */
    val categories: List<JunkCategory> get() = JunkCategory.entries.filter { cat -> items.any { it.category == cat } }

    fun bytesIn(category: JunkCategory): Long = items.filter { it.category == category }.sumOf { it.sizeBytes }

    fun without(paths: Set<String>): ScanResult = copy(items = items.filterNot { it.path in paths })
}

/** Live numbers while a scan runs. */
data class ScanProgress(
    val currentPath: String = "",
    val filesScanned: Int = 0,
    val foundBytes: Long = 0,
    val foundByCategory: Map<JunkCategory, Long> = emptyMap(),
    val startedAt: Long = System.currentTimeMillis(),
)

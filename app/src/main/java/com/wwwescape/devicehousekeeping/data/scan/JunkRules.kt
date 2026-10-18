package com.wwwescape.devicehousekeeping.data.scan

/** What a single file is, according to [JunkRules.classifyFile]. */
sealed interface FileVerdict {
    data class Junk(val category: JunkCategory, val detail: ItemDetail? = null) : FileVerdict

    /** An installer file — its category depends on what's installed, decided later. */
    data object ApkCandidate : FileVerdict
}

/**
 * The scanner's file-level rules, kept free of Android APIs so they can be unit tested. Every
 * rule is a positive match on something unambiguous (an exact name, a reserved extension, a
 * MediaStore prefix); anything not matched is left alone.
 */
object JunkRules {

    /** Top-level shared-storage folders that hold the user's own content. Name-based rules that
     * could in theory collide with a real document (logs) don't apply anywhere inside them. */
    val USER_CONTENT_DIRS = setOf(
        "dcim", "pictures", "movies", "music", "documents", "download", "downloads", "recordings",
        "audiobooks", "podcasts", "ringtones", "alarms", "notifications",
    )

    /** Folders the cleaner refuses to delete as a whole, wherever an item claims they are junk.
     * Paths are relative to a volume root, lowercase. */
    val PROTECTED_RELATIVE_DIRS = USER_CONTENT_DIRS + setOf("android", "android/data", "android/media", "android/obb")

    const val THUMBNAIL_DIR = ".thumbnails"

    private val FOREIGN_NAMES = setOf("thumbs.db", "ehthumbs.db", "ehthumbs_vista.db", ".ds_store", "desktop.ini")

    /** AppleDouble ("._name") files only ever hold a few KB of Finder metadata. Anything bigger
     * that happens to start with "._" is left alone. */
    private const val APPLE_DOUBLE_MAX_BYTES = 64L * 1024

    private val TEMP_SUFFIXES = listOf(".tmp", ".temp", ".crdownload", ".part", ".partial")

    private val LOG_SUFFIXES = listOf(".log", ".hprof", ".dmp")
    private val ROTATED_LOG = Regex(""".*\.log\.\d{1,3}$""")

    /** MediaStore names items it's holding back as `.pending-<expiry seconds>-name` and items in
     * the recycle bin as `.trashed-<expiry seconds>-name` (Android 11+). */
    private val MEDIASTORE_PREFIX = Regex("""^\.(pending|trashed)-(\d{9,11})-.+""")

    /** A pending file is normally finalized within seconds; one that's sat for a day is an
     * abandoned write, regardless of the user's minimum-age setting. */
    private const val PENDING_MIN_AGE_MILLIS = 24L * 60 * 60 * 1000

    private val PACKAGE_NAME = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$""")

    private val MEDIA_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "heic", "heif", "webp", "gif", "dng", "raw", "mp4", "mkv", "mov", "3gp",
        "webm", "avi", "mp3", "m4a", "aac", "opus", "ogg", "wav", "flac", "amr",
    )

    /**
     * @param topLevelDir the first path segment below the volume root, lowercase, or null when
     *   the file sits directly in the root.
     */
    fun classifyFile(
        name: String,
        topLevelDir: String?,
        sizeBytes: Long,
        lastModified: Long,
        now: Long,
        minAgeMillis: Long,
    ): FileVerdict? {
        val lower = name.lowercase()
        val age = now - lastModified

        MEDIASTORE_PREFIX.matchEntire(name)?.let { match ->
            val expiresAt = match.groupValues[2].toLongOrNull()?.times(1000)
            return when (match.groupValues[1]) {
                "trashed" -> FileVerdict.Junk(JunkCategory.TRASH, ItemDetail.Trashed(expiresAt))
                else -> {
                    val expired = expiresAt != null && expiresAt < now
                    if (expired || age >= maxOf(minAgeMillis, PENDING_MIN_AGE_MILLIS)) {
                        FileVerdict.Junk(JunkCategory.TEMP_FILES, ItemDetail.Pending(expiresAt))
                    } else {
                        null
                    }
                }
            }
        }

        if (lower in FOREIGN_NAMES) return FileVerdict.Junk(JunkCategory.FOREIGN_FILES)
        if (name.startsWith("._") && name.length > 2 && sizeBytes <= APPLE_DOUBLE_MAX_BYTES) {
            return FileVerdict.Junk(JunkCategory.FOREIGN_FILES)
        }

        if (lower.endsWith(".apk")) return FileVerdict.ApkCandidate

        if (age < minAgeMillis) return null

        if (TEMP_SUFFIXES.any { lower.endsWith(it) } && lower.length > 5) {
            return FileVerdict.Junk(JunkCategory.TEMP_FILES)
        }

        val inUserContent = topLevelDir != null && topLevelDir in USER_CONTENT_DIRS
        if (!inUserContent && (LOG_SUFFIXES.any { lower.endsWith(it) } || ROTATED_LOG.matches(lower))) {
            return FileVerdict.Junk(JunkCategory.LOGS)
        }
        return null
    }

    fun isThumbnailDir(name: String): Boolean = name == THUMBNAIL_DIR

    fun looksLikePackageName(name: String): Boolean = PACKAGE_NAME.matches(name)

    fun isMediaFile(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in MEDIA_EXTENSIONS

    /** True when [relativePath] (relative to a volume root, '/'-separated) is a folder the
     * cleaner must never delete wholesale. */
    fun isProtectedDir(relativePath: String): Boolean {
        val normalized = relativePath.trim('/').lowercase()
        return normalized.isEmpty() || normalized in PROTECTED_RELATIVE_DIRS
    }
}

package com.wwwescape.devicehousekeeping.data.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import java.io.File

/** Internal storage as the system's own Storage settings report it. */
data class StorageSnapshot(val totalBytes: Long = 0, val freeBytes: Long = 0) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0)
    val usedFraction: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

/** A shared-storage volume the scanner can walk with plain file APIs. */
data class ScanVolume(val root: File, val label: String, val isPrimary: Boolean)

object StorageRepository {

    /** StorageStatsManager reports the marketed capacity (e.g. 128 GB) including the system
     * partition, matching Settings → Storage; StatFs alone would under-report it. */
    fun internal(context: Context): StorageSnapshot {
        val stats = context.getSystemService(StorageStatsManager::class.java)
        val fromStats = runCatching {
            StorageSnapshot(
                totalBytes = stats.getTotalBytes(StorageManager.UUID_DEFAULT),
                freeBytes = stats.getFreeBytes(StorageManager.UUID_DEFAULT),
            )
        }.getOrNull()
        if (fromStats != null && fromStats.totalBytes > 0) return fromStats
        val statFs = StatFs(Environment.getDataDirectory().path)
        return StorageSnapshot(totalBytes = statFs.totalBytes, freeBytes = statFs.availableBytes)
    }

    /** Free bytes summed across [volumes] — sampled before and after cleaning to report the real,
     * measured gain rather than just the sum of deleted file sizes. */
    fun freeBytes(volumes: List<File>): Long =
        volumes.sumOf { root -> runCatching { StatFs(root.path).availableBytes }.getOrDefault(0L) }

    /** Volumes the app can read and delete from directly. Removable volumes are only writable
     * through plain file APIs with all-files access (Android 11+). */
    fun scanVolumes(context: Context, includeRemovable: Boolean): List<ScanVolume> {
        val storageManager = context.getSystemService(StorageManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return storageManager.storageVolumes
                .filter { it.state == Environment.MEDIA_MOUNTED && (it.isPrimary || includeRemovable) }
                .mapNotNull { volume ->
                    val dir = volume.directory ?: return@mapNotNull null
                    ScanVolume(root = dir, label = volume.getDescription(context), isPrimary = volume.isPrimary)
                }
                .sortedByDescending { it.isPrimary }
        }
        @Suppress("DEPRECATION")
        val primary = Environment.getExternalStorageDirectory()
        val label = runCatching { storageManager.primaryStorageVolume.getDescription(context) }.getOrDefault("")
        return listOf(ScanVolume(root = primary, label = label, isPrimary = true))
    }
}

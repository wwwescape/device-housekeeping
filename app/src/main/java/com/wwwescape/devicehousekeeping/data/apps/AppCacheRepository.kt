package com.wwwescape.devicehousekeeping.data.apps

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppCacheEntry(
    val packageName: String,
    val label: String,
    /** All cached data: internal + external cache and code cache. */
    val cacheBytes: Long,
    /** The part of [cacheBytes] in shared storage — what the system "clear app caches" dialog
     * removes. Only reported on Android 12+. */
    val externalCacheBytes: Long?,
    val appBytes: Long,
    val dataBytes: Long,
    val isSystem: Boolean,
)

data class AppCacheSummary(
    val apps: List<AppCacheEntry>,
    val totalCacheBytes: Long,
    val totalExternalCacheBytes: Long?,
)

/**
 * Reads per-app storage use through [StorageStatsManager], which needs Usage access. Read-only:
 * Android doesn't let third-party apps clear other apps' internal caches (that's what the system
 * trims automatically when space runs low), so the screen built on this links each app to its
 * system App info page instead of pretending otherwise.
 */
object AppCacheRepository {

    suspend fun load(context: Context): AppCacheSummary = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val stats = context.getSystemService(StorageStatsManager::class.java)
        val user = Process.myUserHandle()
        val apps = installedApplications(pm).mapNotNull { info ->
            val s = runCatching { stats.queryStatsForPackage(StorageManager.UUID_DEFAULT, info.packageName, user) }
                .getOrNull() ?: return@mapNotNull null
            AppCacheEntry(
                packageName = info.packageName,
                label = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(info.packageName),
                cacheBytes = s.cacheBytes,
                externalCacheBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) s.externalCacheBytes else null,
                appBytes = s.appBytes,
                dataBytes = s.dataBytes,
                isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0 && info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0,
            )
        }
            .filter { it.cacheBytes > 0 }
            .sortedByDescending { it.cacheBytes }
        AppCacheSummary(
            apps = apps,
            totalCacheBytes = apps.sumOf { it.cacheBytes },
            totalExternalCacheBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) apps.sumOf { it.externalCacheBytes ?: 0L } else null,
        )
    }

    private fun installedApplications(pm: PackageManager): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
}

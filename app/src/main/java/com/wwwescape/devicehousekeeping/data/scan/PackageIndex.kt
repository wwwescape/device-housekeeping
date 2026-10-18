package com.wwwescape.devicehousekeeping.data.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

/**
 * A snapshot of what's installed, used to tell an installed app's folder from a leftover one and
 * to compare installer files with installed versions.
 */
class PackageIndex(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** Includes apps uninstalled with "keep data" — their folders are not leftovers. */
    private val installed: Map<String, PackageInfo> = runCatching {
        installedPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES).associateBy { it.packageName }
    }.getOrDefault(emptyMap())

    /**
     * Leftover detection is only trustworthy if every installed app is visible to us. On Android
     * 11+ that needs QUERY_ALL_PACKAGES; without it, most apps would look "uninstalled" and their
     * folders would be wrongly offered. The size floor is a second, independent sanity check —
     * every real device has far more than this many packages, system ones included.
     */
    val canSeeAllPackages: Boolean = run {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            context.checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_GRANTED
        permissionOk && installed.size >= MIN_PLAUSIBLE_PACKAGES && "android" in installed
    }

    fun isInstalled(packageName: String): Boolean {
        if (packageName in installed) return true
        // Re-check live: the app may have been installed since the snapshot was taken.
        return runCatching { packageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES) }.isSuccess
    }

    fun installedVersion(packageName: String): Pair<Long, String?>? {
        val info = runCatching { packageInfo(packageName, 0) }.getOrNull() ?: return null
        return PackageInfoCompat.getLongVersionCode(info) to info.versionName
    }

    fun label(packageName: String): String? = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    /** Reads an installer file's manifest without installing it. Null when the file isn't a
     * valid APK (often an incomplete download). */
    fun readArchive(path: String): ItemDetail.Apk? {
        val info = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(path, 0)
            }
        }.getOrNull() ?: return null
        val label = info.applicationInfo?.let { appInfo ->
            appInfo.sourceDir = path
            appInfo.publicSourceDir = path
            runCatching { appInfo.loadLabel(pm).toString() }.getOrNull()
        }
        val installedVersion = installedVersion(info.packageName)
        return ItemDetail.Apk(
            packageName = info.packageName,
            label = label,
            apkVersionName = info.versionName,
            apkVersionCode = PackageInfoCompat.getLongVersionCode(info),
            installedVersionName = installedVersion?.second,
            installedVersionCode = installedVersion?.first,
        )
    }

    private fun installedPackages(flags: Int): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }

    private fun packageInfo(packageName: String, flags: Int): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, flags)
        }

    private companion object {
        const val MIN_PLAUSIBLE_PACKAGES = 20
    }
}

/** True when an installer file is redundant: the same app is installed at the same or a newer
 * version. */
fun ItemDetail.Apk.isAlreadyInstalled(): Boolean =
    installedVersionCode != null && installedVersionCode >= apkVersionCode

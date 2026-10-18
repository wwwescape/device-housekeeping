package com.wwwescape.devicehousekeeping.data.schedule

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wwwescape.devicehousekeeping.data.clean.CleanEvent
import com.wwwescape.devicehousekeeping.data.clean.Cleaner
import com.wwwescape.devicehousekeeping.data.history.HistoryEntry
import com.wwwescape.devicehousekeeping.data.history.HistoryKind
import com.wwwescape.devicehousekeeping.data.history.HistoryRepository
import com.wwwescape.devicehousekeeping.data.scan.JunkItem
import com.wwwescape.devicehousekeeping.data.scan.JunkScanner
import com.wwwescape.devicehousekeeping.data.scan.Safety
import com.wwwescape.devicehousekeeping.data.scan.ScanEvent
import com.wwwescape.devicehousekeeping.data.settings.AutoCleanInterval
import com.wwwescape.devicehousekeeping.data.settings.SettingsRepository
import com.wwwescape.devicehousekeeping.data.storage.StorageAccess
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** What an unattended run may delete: Safe categories only. Review items always need a person. */
fun autoCleanable(items: List<JunkItem>): List<JunkItem> = items.filter { it.category.safety == Safety.SAFE }

object AutoCleanScheduler {

    private const val WORK_NAME = "auto_clean"

    /** Schedules (or cancels) the periodic job to match [interval]. Safe to call repeatedly:
     * an unchanged interval keeps the existing schedule. */
    fun apply(context: Context, interval: AutoCleanInterval) {
        val workManager = WorkManager.getInstance(context)
        val days = interval.days
        if (days == null) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        // Deliberately no "storage not low" constraint: low storage is exactly when cleaning
        // matters. Battery-not-low keeps the walk from running on a nearly flat phone.
        val request = PeriodicWorkRequestBuilder<AutoCleanWorker>(days, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setInitialDelay(days, TimeUnit.DAYS)
            .addTag(interval.name)
            .build()
        val existing = runCatching { workManager.getWorkInfosForUniqueWork(WORK_NAME).get() }.getOrDefault(emptyList())
        val sameInterval = existing.any { !it.state.isFinished && interval.name in it.tags }
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (sameInterval) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

/**
 * Scans with the user's current settings (minimum age, exclusions, removable storage) and cleans
 * the Safe items through the same [Cleaner] — and therefore the same SafeDeleter re-checks — as
 * an interactive run. Does nothing without storage access.
 */
class AutoCleanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settings = SettingsRepository.current(context).cleaning
        if (settings.autoClean == AutoCleanInterval.OFF || !StorageAccess.hasStorageAccess(context)) return Result.success()

        val result = JunkScanner(context).scan(settings).filterIsInstance<ScanEvent.Finished>().first().result
        val items = autoCleanable(result.items)
        if (items.isEmpty()) return Result.success()

        val finished = Cleaner(context).clean(items, settings.excludedPaths).filterIsInstance<CleanEvent.Finished>().first()
        val report = finished.report
        if (report.deletedItems > 0) {
            HistoryRepository.add(
                context,
                HistoryEntry(
                    timestamp = report.finishedAt,
                    kind = HistoryKind.AUTO_CLEAN,
                    freedBytes = report.freedBytes,
                    measuredFreedBytes = report.measuredFreedBytes,
                    itemCount = report.deletedItems,
                    byCategory = report.freedByCategory,
                ),
            )
            AutoCleanNotifier.notifyCleaned(context, report.freedBytes, report.deletedItems)
        }
        return Result.success()
    }
}

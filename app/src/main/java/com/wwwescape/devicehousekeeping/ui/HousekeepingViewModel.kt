package com.wwwescape.devicehousekeeping.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.devicehousekeeping.data.apps.AppCacheRepository
import com.wwwescape.devicehousekeeping.data.apps.AppCacheSummary
import com.wwwescape.devicehousekeeping.data.clean.CleanEvent
import com.wwwescape.devicehousekeeping.data.clean.CleanProgress
import com.wwwescape.devicehousekeeping.data.clean.CleanReport
import com.wwwescape.devicehousekeeping.data.clean.Cleaner
import com.wwwescape.devicehousekeeping.data.history.History
import com.wwwescape.devicehousekeeping.data.history.HistoryEntry
import com.wwwescape.devicehousekeeping.data.history.HistoryKind
import com.wwwescape.devicehousekeeping.data.history.HistoryRepository
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.data.scan.JunkItem
import com.wwwescape.devicehousekeeping.data.scan.JunkScanner
import com.wwwescape.devicehousekeeping.data.scan.Safety
import com.wwwescape.devicehousekeeping.data.scan.ScanEvent
import com.wwwescape.devicehousekeeping.data.scan.ScanProgress
import com.wwwescape.devicehousekeeping.data.scan.ScanResult
import com.wwwescape.devicehousekeeping.data.settings.AppSettings
import com.wwwescape.devicehousekeeping.data.settings.SettingsRepository
import com.wwwescape.devicehousekeeping.data.storage.StorageAccess
import com.wwwescape.devicehousekeeping.data.storage.StorageRepository
import com.wwwescape.devicehousekeeping.data.storage.StorageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The scan → review → clean → result flow shown on Home. */
sealed interface CleanerPhase {
    data object Idle : CleanerPhase
    data class Scanning(val progress: ScanProgress) : CleanerPhase
    data class Results(val result: ScanResult) : CleanerPhase
    data class Cleaning(val progress: CleanProgress) : CleanerPhase
    data class Done(val report: CleanReport) : CleanerPhase
}

sealed interface AppCacheState {
    data object Loading : AppCacheState
    data object NoAccess : AppCacheState
    data class Loaded(val summary: AppCacheSummary) : AppCacheState
}

/** Shared by Home and the category screens (activity-scoped), so selection made in a category's
 * detail list is the same selection the Clean button acts on. */
class HousekeepingViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<Application>()
    private val scanner = JunkScanner(application)
    private val cleaner = Cleaner(application)

    val settings: StateFlow<AppSettings> = SettingsRepository.settingsFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val history: StateFlow<History> = HistoryRepository.historyFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, History())

    private val _storage = MutableStateFlow(StorageSnapshot())
    val storage: StateFlow<StorageSnapshot> = _storage.asStateFlow()

    private val _phase = MutableStateFlow<CleanerPhase>(CleanerPhase.Idle)
    val phase: StateFlow<CleanerPhase> = _phase.asStateFlow()

    /** Paths of the items the user has ticked. */
    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection.asStateFlow()

    private val _appCaches = MutableStateFlow<AppCacheState>(AppCacheState.Loading)
    val appCaches: StateFlow<AppCacheState> = _appCaches.asStateFlow()

    /** Measured gain of the last system "clear app caches" run, shown once then cleared. */
    private val _systemCacheFreed = MutableStateFlow<Long?>(null)
    val systemCacheFreed: StateFlow<Long?> = _systemCacheFreed.asStateFlow()

    private var job: Job? = null

    init {
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch(Dispatchers.IO) { _storage.value = StorageRepository.internal(app) }
    }

    fun startScan() {
        if (!StorageAccess.hasStorageAccess(app)) return
        job?.cancel()
        _phase.value = CleanerPhase.Scanning(ScanProgress())
        job = viewModelScope.launch {
            val cleaning = SettingsRepository.current(app).cleaning
            val startedAt = System.currentTimeMillis()
            scanner.scan(cleaning).collect { event ->
                when (event) {
                    is ScanEvent.Progress -> _phase.value = CleanerPhase.Scanning(event.progress)
                    is ScanEvent.Finished -> {
                        // A scan of a tidy phone can finish in a blink; hold the radar on screen
                        // long enough to read as "something was checked".
                        val elapsed = System.currentTimeMillis() - startedAt
                        if (elapsed < MIN_SCAN_DISPLAY_MILLIS) delay(MIN_SCAN_DISPLAY_MILLIS - elapsed)
                        _selection.value = event.result.items.filter { it.category.safety == Safety.SAFE }.map { it.path }.toSet()
                        _phase.value = CleanerPhase.Results(event.result)
                    }
                }
            }
        }
    }

    fun cancelScan() {
        job?.cancel()
        _phase.value = CleanerPhase.Idle
    }

    fun toggle(path: String) {
        _selection.value = _selection.value.let { if (path in it) it - path else it + path }
    }

    fun setSelected(items: List<JunkItem>, selected: Boolean) {
        val paths = items.map { it.path }
        _selection.value = if (selected) _selection.value + paths else _selection.value - paths.toSet()
    }

    fun setCategorySelected(category: JunkCategory, selected: Boolean) {
        val result = (_phase.value as? CleanerPhase.Results)?.result ?: return
        setSelected(result.itemsIn(category), selected)
    }

    fun selectedItems(): List<JunkItem> {
        val result = (_phase.value as? CleanerPhase.Results)?.result ?: return emptyList()
        val selected = _selection.value
        return result.items.filter { it.path in selected }
    }

    fun clean() {
        val items = selectedItems()
        if (items.isEmpty()) return
        job?.cancel()
        _phase.value = CleanerPhase.Cleaning(CleanProgress(itemsTotal = items.size, bytesTotal = items.sumOf { it.sizeBytes }))
        job = viewModelScope.launch {
            cleaner.clean(items, settings.value.cleaning.excludedPaths).collect { event ->
                when (event) {
                    is CleanEvent.Progress -> _phase.value = CleanerPhase.Cleaning(event.progress)
                    is CleanEvent.Finished -> {
                        val report = event.report
                        if (report.deletedItems > 0) {
                            HistoryRepository.add(
                                app,
                                HistoryEntry(
                                    timestamp = report.finishedAt,
                                    kind = HistoryKind.CLEAN,
                                    freedBytes = report.freedBytes,
                                    measuredFreedBytes = report.measuredFreedBytes,
                                    itemCount = report.deletedItems,
                                    byCategory = report.freedByCategory,
                                ),
                            )
                        }
                        _selection.value = emptySet()
                        _phase.value = CleanerPhase.Done(report)
                        refreshStorage()
                    }
                }
            }
        }
    }

    /** Back to the idle Scan button after the result screen. */
    fun finish() {
        _phase.value = CleanerPhase.Idle
        refreshStorage()
    }

    /** Adds [item] to the exclusion list and drops it from the current results. */
    fun exclude(item: JunkItem) {
        viewModelScope.launch { SettingsRepository.addExcludedPath(app, item.path) }
        val current = _phase.value
        if (current is CleanerPhase.Results) {
            _phase.value = CleanerPhase.Results(current.result.without(setOf(item.path)))
        }
        _selection.value = _selection.value - item.path
    }

    fun loadAppCaches() {
        if (!StorageAccess.hasUsageAccess(app)) {
            _appCaches.value = AppCacheState.NoAccess
            return
        }
        if (_appCaches.value !is AppCacheState.Loaded) _appCaches.value = AppCacheState.Loading
        viewModelScope.launch {
            _appCaches.value = AppCacheState.Loaded(AppCacheRepository.load(app))
        }
    }

    /** Free bytes on internal shared storage, sampled before launching the system dialog. */
    fun sampleFreeBytes(): Long = StorageRepository.freeBytes(listOf(primaryRoot()))

    fun onSystemCacheCleared(freeBefore: Long) {
        viewModelScope.launch {
            delay(SYSTEM_CLEAR_SETTLE_MILLIS)
            val freed = withContext(Dispatchers.IO) { (sampleFreeBytes() - freeBefore).coerceAtLeast(0) }
            HistoryRepository.add(
                app,
                HistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    kind = HistoryKind.SYSTEM_CACHE,
                    freedBytes = freed,
                    measuredFreedBytes = freed,
                    itemCount = 0,
                ),
            )
            _systemCacheFreed.value = freed
            refreshStorage()
            loadAppCaches()
        }
    }

    fun consumeSystemCacheFreed() {
        _systemCacheFreed.value = null
    }

    fun clearHistory() {
        viewModelScope.launch { HistoryRepository.clear(app) }
    }

    @Suppress("DEPRECATION")
    private fun primaryRoot() = Environment.getExternalStorageDirectory()

    private companion object {
        const val MIN_SCAN_DISPLAY_MILLIS = 1800L
        const val SYSTEM_CLEAR_SETTLE_MILLIS = 800L
    }
}

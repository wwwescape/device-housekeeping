package com.wwwescape.devicehousekeeping.data.history

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyDataStore by preferencesDataStore(name = "history")

enum class HistoryKind {
    /** A scan-and-clean run in this app. */
    CLEAN,

    /** The system "clear app caches" dialog, launched from this app. */
    SYSTEM_CACHE,

    /** A scheduled background cleaning of Safe categories. */
    AUTO_CLEAN,
}

data class HistoryEntry(
    val timestamp: Long,
    val kind: HistoryKind,
    val freedBytes: Long,
    val measuredFreedBytes: Long,
    val itemCount: Int,
    val byCategory: Map<JunkCategory, Long> = emptyMap(),
)

data class History(
    val entries: List<HistoryEntry> = emptyList(),
    /** Kept separately from [entries] so it survives the entry cap. */
    val lifetimeFreedBytes: Long = 0,
    val lifetimeRuns: Long = 0,
)

/** A local log of what each cleaning freed. Nothing leaves the device. */
object HistoryRepository {

    private val ENTRIES_KEY = stringPreferencesKey("entries")
    private val LIFETIME_BYTES_KEY = longPreferencesKey("lifetime_bytes")
    private val LIFETIME_RUNS_KEY = longPreferencesKey("lifetime_runs")
    private const val MAX_ENTRIES = 200

    fun historyFlow(context: Context): Flow<History> = context.historyDataStore.data.map { prefs ->
        History(
            entries = decode(prefs[ENTRIES_KEY]),
            lifetimeFreedBytes = prefs[LIFETIME_BYTES_KEY] ?: 0L,
            lifetimeRuns = prefs[LIFETIME_RUNS_KEY] ?: 0L,
        )
    }

    suspend fun add(context: Context, entry: HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val entries = (listOf(entry) + decode(prefs[ENTRIES_KEY])).take(MAX_ENTRIES)
            prefs[ENTRIES_KEY] = encode(entries)
            prefs[LIFETIME_BYTES_KEY] = (prefs[LIFETIME_BYTES_KEY] ?: 0L) + entry.headlineBytes
            prefs[LIFETIME_RUNS_KEY] = (prefs[LIFETIME_RUNS_KEY] ?: 0L) + 1
        }
    }

    suspend fun clear(context: Context) {
        context.historyDataStore.edit { it.clear() }
    }

    private fun encode(entries: List<HistoryEntry>): String = JSONArray().apply {
        entries.forEach { entry ->
            put(
                JSONObject()
                    .put("t", entry.timestamp)
                    .put("k", entry.kind.name)
                    .put("f", entry.freedBytes)
                    .put("m", entry.measuredFreedBytes)
                    .put("n", entry.itemCount)
                    .put("c", JSONObject().apply { entry.byCategory.forEach { (cat, bytes) -> put(cat.name, bytes) } }),
            )
        }
    }.toString()

    private fun decode(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val categories = obj.optJSONObject("c")
                HistoryEntry(
                    timestamp = obj.optLong("t"),
                    kind = runCatching { HistoryKind.valueOf(obj.optString("k")) }.getOrDefault(HistoryKind.CLEAN),
                    freedBytes = obj.optLong("f"),
                    measuredFreedBytes = obj.optLong("m"),
                    itemCount = obj.optInt("n"),
                    byCategory = buildMap {
                        categories?.keys()?.forEach { key ->
                            runCatching { JunkCategory.valueOf(key) }.getOrNull()?.let { put(it, categories.optLong(key)) }
                        }
                    },
                )
            }
        }.getOrDefault(emptyList())
    }
}

/** The number shown for an entry: the deleted size for in-app runs, the measured gain for the
 * system dialog (which doesn't report what it deleted). */
val HistoryEntry.headlineBytes: Long get() = if (kind == HistoryKind.SYSTEM_CACHE) measuredFreedBytes else freedBytes

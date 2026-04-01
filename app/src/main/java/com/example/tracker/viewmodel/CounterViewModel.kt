package com.example.tracker.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tracker.data.Counter
import com.example.tracker.data.CounterGroup
import com.example.tracker.data.CounterList
import com.example.tracker.data.CustomOrderEntity
import com.example.tracker.data.TrackerDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class SortOrder {
    CUSTOM,
    VALUE_HIGH_LOW,
    VALUE_LOW_HIGH,
    ALPHA_AZ,
    ALPHA_ZA
}

/** A single item in the unified display list. */
sealed class DisplayItem {
    /** Represents a group card (may contain multiple counters). */
    data class Group(val group: CounterGroup) : DisplayItem()
    /** Represents a single ungrouped counter rendered as its own card. */
    data class UngroupedCounter(val counter: Counter) : DisplayItem()
}

class CounterViewModel(private val db: TrackerDatabase, context: Context) : ViewModel() {

    private val dataStore = context.dataStore

    companion object {
        private val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
    }

    // ── Lists ─────────────────────────────────────────────────────────────────

    private val _lists = mutableStateListOf<CounterList>()
    val lists: List<CounterList> = _lists

    private val _activeListId = mutableStateOf("")
    val activeListId = _activeListId

    /**
     * Per-list custom order cache (not observable — only _customOrder is observed by Compose).
     * Entries are "g:<groupId>" / "c:<counterId>" tokens, same as _customOrder.
     */
    private val _customOrderCache = HashMap<String, MutableList<String>>()

    private var listSequence = 0

    // ── Counters / Groups ─────────────────────────────────────────────────────

    private val _counters = mutableStateListOf<Counter>()
    val counters: List<Counter> = _counters

    private val _groups = mutableStateListOf<CounterGroup>()
    val groups: List<CounterGroup> = _groups

    /**
     * Custom order for the **currently active list**.
     * Swapped in/out by switchActiveList().
     */
    private val _customOrder = mutableStateListOf<String>()

    private var counterSequence = 0
    private var groupSequence   = 0

    private val _sortOrder = mutableStateOf(SortOrder.CUSTOM)
    val sortOrder = _sortOrder

    private val _valueSortSnapshot = mutableStateMapOf<String, Int>()
    private var _valueSortDebounceJob: Job? = null

    private fun takeValueSortSnapshot() {
        val activeId = _activeListId.value
        _counters.filter { it.listId == activeId }.forEach { _valueSortSnapshot[it.id] = it.value }
    }

    private fun scheduleValueSortSnapshot() {
        if (_sortOrder.value != SortOrder.VALUE_HIGH_LOW &&
            _sortOrder.value != SortOrder.VALUE_LOW_HIGH) return
        _valueSortDebounceJob?.cancel()
        _valueSortDebounceJob = viewModelScope.launch {
            delay(3_000)
            takeValueSortSnapshot()
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        if (order == SortOrder.VALUE_HIGH_LOW || order == SortOrder.VALUE_LOW_HIGH) {
            _valueSortDebounceJob?.cancel()
            takeValueSortSnapshot()
        }
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_SORT_ORDER] = order.name }
        }
    }

    /** Key of the most recently added item ("g:<id>" or "c:<id>"). Null after consumed. */
    private val _lastAddedKey = mutableStateOf<String?>(null)
    val lastAddedKey = _lastAddedKey

    /** Call this after the UI has scrolled to the new item. */
    fun consumeLastAddedKey() { _lastAddedKey.value = null }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private fun saveOrder() {
        val listId = _activeListId.value
        if (listId.isBlank()) return
        viewModelScope.launch {
            db.customOrderDao().saveOrder(CustomOrderEntity(listId = listId, order = _customOrder.joinToString(",")))
        }
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            // Restore sort order
            val savedSortOrder = dataStore.data.first()[KEY_SORT_ORDER]
            if (savedSortOrder != null) {
                _sortOrder.value = runCatching { SortOrder.valueOf(savedSortOrder) }
                    .getOrDefault(SortOrder.CUSTOM)
            }

            val savedLists    = db.counterListDao().getAllFlow().first()
            val savedGroups   = db.counterGroupDao().getAllFlow().first()
            val savedCounters = db.counterDao().getAllFlow().first()
            val savedOrders   = db.customOrderDao().getAllOrders()

            // Populate per-list custom order cache
            savedOrders.forEach { entity ->
                _customOrderCache[entity.listId] =
                    entity.order.split(",").filter { it.isNotBlank() }.toMutableList()
            }

            if (savedLists.isEmpty()) {
                // First launch — create the Default list then seed three counters
                val defaultList = CounterList(
                    id       = UUID.randomUUID().toString(),
                    name     = "Default",
                    position = 0
                )
                _lists.add(defaultList)
                _activeListId.value = defaultList.id
                _customOrderCache[defaultList.id] = mutableListOf()
                listSequence = 1
                db.counterListDao().upsert(defaultList)
                repeat(3) { addCounter() }
            } else {
                _lists.addAll(savedLists)
                _groups.addAll(savedGroups)
                _counters.addAll(savedCounters)
                listSequence    = savedLists.size
                groupSequence   = savedGroups.size
                counterSequence = savedCounters.size

                _activeListId.value = savedLists.first().id
                _customOrder.addAll(_customOrderCache[_activeListId.value] ?: emptyList())
                rebuildCustomOrder()
            }

            // Initialise snapshot if app is launched already in value-sort mode
            if (_sortOrder.value == SortOrder.VALUE_HIGH_LOW ||
                _sortOrder.value == SortOrder.VALUE_LOW_HIGH) {
                takeValueSortSnapshot()
            }
        }
    }

    // ── List CRUD ─────────────────────────────────────────────────────────────

    fun addList(name: String? = null) {
        listSequence++
        val resolvedName = name ?: "List $listSequence"
        val list = CounterList(
            id       = UUID.randomUUID().toString(),
            name     = resolvedName,
            position = _lists.size
        )
        _lists.add(list)
        _customOrderCache[list.id] = mutableListOf()
        viewModelScope.launch { db.counterListDao().upsert(list) }
        switchActiveList(list.id)
    }

    fun renameList(listId: String, name: String) {
        val i = _lists.indexOfFirst { it.id == listId }
        if (i == -1) return
        _lists[i] = _lists[i].copy(name = name)
        viewModelScope.launch { db.counterListDao().upsert(_lists[i]) }
    }

    fun removeList(listId: String) {
        if (_lists.size <= 1) return   // guard: never delete the last list
        val wasActive = _activeListId.value == listId

        // Save current order to cache before mutating
        if (wasActive) _customOrderCache[listId] = _customOrder.toMutableList()

        _groups.removeAll { it.listId == listId }
        _counters.removeAll { it.listId == listId }
        _customOrderCache.remove(listId)
        _lists.removeAll { it.id == listId }

        viewModelScope.launch {
            db.counterDao().deleteByListId(listId)
            db.counterGroupDao().deleteByListId(listId)
            db.customOrderDao().deleteByListId(listId)
            db.counterListDao().deleteById(listId)
        }

        if (wasActive) {
            val newId = _lists.first().id
            _activeListId.value = newId
            _customOrder.clear()
            _customOrder.addAll(_customOrderCache[newId] ?: emptyList())
            rebuildCustomOrder()
        }
    }

    /**
     * Switch the active list. Saves the current list's order to the cache,
     * then loads the target list's order into _customOrder.
     */
    fun switchActiveList(listId: String) {
        if (listId == _activeListId.value) return
        // Persist the current active list's order in the cache
        _customOrderCache[_activeListId.value] = _customOrder.toMutableList()
        _activeListId.value = listId
        _customOrder.clear()
        _customOrder.addAll(_customOrderCache[listId] ?: emptyList())
        rebuildCustomOrder()
        // Reset value-sort snapshot for the new list
        _valueSortDebounceJob?.cancel()
        if (_sortOrder.value == SortOrder.VALUE_HIGH_LOW ||
            _sortOrder.value == SortOrder.VALUE_LOW_HIGH) {
            takeValueSortSnapshot()
        }
    }

    // ── Custom-order helpers ──────────────────────────────────────────────────

    private fun rebuildCustomOrder() {
        val activeId = _activeListId.value
        val existing = _customOrder.toSet()
        _groups.filter { it.listId == activeId }.forEach { g ->
            val key = "g:${g.id}"
            if (key !in existing) _customOrder.add(key)
        }
        _counters.filter { it.groupId == null && it.listId == activeId }.forEach { c ->
            val key = "c:${c.id}"
            if (key !in existing) _customOrder.add(key)
        }
        val validKeys =
            _groups.filter { it.listId == activeId }.map { "g:${it.id}" }.toSet() +
            _counters.filter { it.groupId == null && it.listId == activeId }.map { "c:${it.id}" }.toSet()
        _customOrder.removeAll { it !in validKeys }
    }

    fun reorderItems(from: Int, to: Int) {
        _sortOrder.value = SortOrder.CUSTOM
        rebuildCustomOrder()
        if (from == to || from !in _customOrder.indices || to !in _customOrder.indices) return
        val item = _customOrder.removeAt(from)
        _customOrder.add(to, item)
        saveOrder()
    }

    // ── Unified sorted display list ───────────────────────────────────────────

    fun sortedDisplayItems(): List<DisplayItem> {
        val activeId     = _activeListId.value
        val groupItems   = _groups.filter { it.listId == activeId }.map { DisplayItem.Group(it) }
        val counterItems = _counters.filter { it.groupId == null && it.listId == activeId }
                                    .map { DisplayItem.UngroupedCounter(it) }
        val all: List<DisplayItem> = groupItems + counterItems

        // Helper: stable sort value for a counter in VALUE mode (uses snapshot so the
        // position only updates after the 3-second debounce, not on every tap).
        fun snapValue(counterId: String, live: Int) =
            _valueSortSnapshot.getOrDefault(counterId, live)

        return when (_sortOrder.value) {
            SortOrder.VALUE_HIGH_LOW -> all.sortedByDescending { item ->
                when (item) {
                    is DisplayItem.Group ->
                        _counters.filter { it.groupId == item.group.id }
                                 .sumOf { snapValue(it.id, it.value) }
                    is DisplayItem.UngroupedCounter ->
                        snapValue(item.counter.id, item.counter.value)
                }
            }
            SortOrder.VALUE_LOW_HIGH -> all.sortedBy { item ->
                when (item) {
                    is DisplayItem.Group ->
                        _counters.filter { it.groupId == item.group.id }
                                 .sumOf { snapValue(it.id, it.value) }
                    is DisplayItem.UngroupedCounter ->
                        snapValue(item.counter.id, item.counter.value)
                }
            }
            SortOrder.ALPHA_AZ -> all.sortedBy { item ->
                when (item) {
                    is DisplayItem.Group            -> item.group.name.lowercase()
                    is DisplayItem.UngroupedCounter -> item.counter.name.lowercase()
                }
            }
            SortOrder.ALPHA_ZA -> all.sortedByDescending { item ->
                when (item) {
                    is DisplayItem.Group            -> item.group.name.lowercase()
                    is DisplayItem.UngroupedCounter -> item.counter.name.lowercase()
                }
            }
            SortOrder.CUSTOM -> {
                rebuildCustomOrder()
                val activeGroups   = _groups.filter { it.listId == activeId }.associateBy { "g:${it.id}" }
                val activeCounters = _counters.filter { it.groupId == null && it.listId == activeId }.associateBy { "c:${it.id}" }
                _customOrder.mapNotNull { key ->
                    activeGroups[key]?.let   { DisplayItem.Group(it) }
                    ?: activeCounters[key]?.let { DisplayItem.UngroupedCounter(it) }
                }
            }
        }
    }

    // ── Legacy helpers ────────────────────────────────────────────────────────
    fun getCountersInGroup(groupId: String): List<Counter> = _counters.filter { it.groupId == groupId }
    fun getUngroupedCounters(): List<Counter> = _counters.filter { it.groupId == null && it.listId == _activeListId.value }

    // ── Counter CRUD ──────────────────────────────────────────────────────────

    fun addCounter(name: String? = null, groupId: String? = null) {
        counterSequence++
        val activeId = _activeListId.value
        val counter = Counter(
            id      = UUID.randomUUID().toString(),
            name    = name ?: "Counter $counterSequence",
            value   = 0,
            groupId = groupId,
            color   = null,
            listId  = activeId
        )
        _counters.add(counter)
        if (groupId == null) {
            _customOrder.add("c:${counter.id}")
            saveOrder()
            _lastAddedKey.value = "c:${counter.id}"
        }
        viewModelScope.launch { db.counterDao().upsert(counter) }
    }

    fun removeCounter(counterId: String) {
        _counters.removeAll { it.id == counterId }
        _customOrder.remove("c:$counterId")
        saveOrder()
        viewModelScope.launch { db.counterDao().deleteById(counterId) }
    }

    fun removeAllCounters() {
        val activeId = _activeListId.value
        val ids = _counters.filter { it.listId == activeId }.map { it.id }.toSet()
        _counters.removeAll { it.listId == activeId }
        _customOrder.removeAll { key -> ids.any { key == "c:$it" } }
        saveOrder()
        viewModelScope.launch { db.counterDao().deleteByListId(activeId) }
    }

    fun incrementCounter(counterId: String) {
        val i = _counters.indexOfFirst { it.id == counterId }
        if (i == -1) return
        _counters[i] = _counters[i].copy(value = _counters[i].value + 1)
        viewModelScope.launch { db.counterDao().upsert(_counters[i]) }
        scheduleValueSortSnapshot()
    }

    fun decrementCounter(counterId: String) {
        val i = _counters.indexOfFirst { it.id == counterId }
        if (i == -1) return
        _counters[i] = _counters[i].copy(value = _counters[i].value - 1)
        viewModelScope.launch { db.counterDao().upsert(_counters[i]) }
        scheduleValueSortSnapshot()
    }

    fun setCounterValue(counterId: String, value: Int) {
        val i = _counters.indexOfFirst { it.id == counterId }
        if (i == -1) return
        _counters[i] = _counters[i].copy(value = value)
        viewModelScope.launch { db.counterDao().upsert(_counters[i]) }
    }

    fun updateCounterName(counterId: String, name: String) {
        val i = _counters.indexOfFirst { it.id == counterId }
        if (i == -1) return
        _counters[i] = _counters[i].copy(name = name)
        viewModelScope.launch { db.counterDao().upsert(_counters[i]) }
    }

    fun updateCounterColor(counterId: String, colorValue: Long?) {
        val i = _counters.indexOfFirst { it.id == counterId }
        if (i == -1) return
        _counters[i] = _counters[i].copy(color = colorValue)
        viewModelScope.launch { db.counterDao().upsert(_counters[i]) }
    }

    fun assignCounterToGroup(counterId: String, groupId: String?) {
        val i = _counters.indexOfFirst { it.id == counterId }
        if (i == -1) return
        val wasUngrouped = _counters[i].groupId == null
        _counters[i] = _counters[i].copy(groupId = groupId)
        val nowUngrouped = groupId == null
        if (wasUngrouped && !nowUngrouped) {
            _customOrder.remove("c:$counterId")
        } else if (!wasUngrouped && nowUngrouped) {
            _customOrder.add("c:$counterId")
        }
        saveOrder()
        viewModelScope.launch { db.counterDao().upsert(_counters[i]) }
    }

    // ── Group CRUD ────────────────────────────────────────────────────────────

    fun addGroup(name: String? = null, colorValue: Long? = null) {
        groupSequence++
        val resolvedName = name ?: "Group $groupSequence"
        val activeId = _activeListId.value
        val chosenColor = colorValue ?: 0L
        val group = CounterGroup(UUID.randomUUID().toString(), resolvedName, chosenColor, activeId)
        _groups.add(group)
        _customOrder.add("g:${group.id}")
        saveOrder()
        _lastAddedKey.value = "g:${group.id}"
        viewModelScope.launch { db.counterGroupDao().upsert(group) }
    }

    fun removeGroup(groupId: String) {
        _groups.removeAll { it.id == groupId }
        _customOrder.remove("g:$groupId")
        _counters.replaceAll { c ->
            if (c.groupId == groupId) {
                val ungrouped = c.copy(groupId = null)
                _customOrder.add("c:${c.id}")
                ungrouped
            } else c
        }
        saveOrder()
        viewModelScope.launch {
            db.counterGroupDao().deleteById(groupId)
            _counters.filter { it.groupId == null }.forEach { db.counterDao().upsert(it) }
        }
    }

    fun updateGroupName(groupId: String, name: String) {
        val i = _groups.indexOfFirst { it.id == groupId }
        if (i == -1) return
        _groups[i] = _groups[i].copy(name = name)
        viewModelScope.launch { db.counterGroupDao().upsert(_groups[i]) }
    }

    fun updateGroupColor(groupId: String, colorValue: Long?) {
        val i = _groups.indexOfFirst { it.id == groupId }
        if (i == -1) return
        _groups[i] = _groups[i].copy(colorValue = colorValue ?: 0L)
        viewModelScope.launch { db.counterGroupDao().upsert(_groups[i]) }
    }

    // ── Export ────────────────────────────────────────────────────────────────

    fun buildCsvExport(): String {
        val activeId = _activeListId.value
        fun csvField(value: String): String =
            if (value.contains(',') || value.contains('"') || value.contains('\n'))
                "\"${value.replace("\"", "\"\"")}\""
            else value

        val sb = StringBuilder()
        sb.appendLine("Group,Counter,Value")

        _counters.filter { it.groupId == null && it.listId == activeId }.forEach { c ->
            sb.appendLine(",${csvField(c.name)},${c.value}")
        }

        _groups.filter { it.listId == activeId }.forEach { g ->
            _counters.filter { it.groupId == g.id }.forEach { c ->
                sb.appendLine("${csvField(g.name)},${csvField(c.name)},${c.value}")
            }
        }

        return sb.toString()
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Parses [csvContent] (in the format produced by [buildCsvExport]) and creates a new
     * [CounterList] named [listName], then switches to it.
     */
    fun importFromCsv(csvContent: String, listName: String = "Imported List") {
        listSequence++
        val list = CounterList(
            id       = UUID.randomUUID().toString(),
            name     = listName,
            position = _lists.size
        )

        val dataLines = csvContent.lines()
            .filter { it.isNotBlank() }
            .let { lines ->
                if (lines.isNotEmpty() && lines.first().trim().equals("Group,Counter,Value", ignoreCase = true))
                    lines.drop(1) else lines
            }

        // Build entities in memory first so all state updates happen synchronously.
        val newGroups   = mutableListOf<CounterGroup>()
        val newCounters = mutableListOf<Counter>()
        val newOrder    = mutableListOf<String>()
        val groupMap    = mutableMapOf<String, CounterGroup>()   // group name → entity
        dataLines.forEach { line ->
            val fields      = parseCsvLine(line)
            if (fields.size < 3) return@forEach
            val groupName   = fields[0].trim()
            val counterName = fields[1].trim()
            val value       = fields[2].trim().toIntOrNull() ?: 0

            if (groupName.isEmpty()) {
                counterSequence++
                val counter = Counter(
                    id      = UUID.randomUUID().toString(),
                    name    = counterName.ifBlank { "Counter $counterSequence" },
                    value   = value,
                    groupId = null,
                    color   = null,
                    listId  = list.id
                )
                newCounters.add(counter)
                newOrder.add("c:${counter.id}")
            } else {
                val group = groupMap.getOrPut(groupName) {
                    groupSequence++
                    val g = CounterGroup(UUID.randomUUID().toString(), groupName, 0L, list.id)
                    newGroups.add(g)
                    newOrder.add("g:${g.id}")
                    g
                }
                counterSequence++
                val counter = Counter(
                    id      = UUID.randomUUID().toString(),
                    name    = counterName.ifBlank { "Counter $counterSequence" },
                    value   = value,
                    groupId = group.id,
                    color   = null,
                    listId  = list.id
                )
                newCounters.add(counter)
            }
        }

        // Update in-memory state
        _lists.add(list)
        _groups.addAll(newGroups)
        _counters.addAll(newCounters)
        _customOrderCache[list.id] = newOrder.toMutableList()

        // Persist to DB
        viewModelScope.launch {
            db.counterListDao().upsert(list)
            newGroups.forEach { db.counterGroupDao().upsert(it) }
            newCounters.forEach { db.counterDao().upsert(it) }
            db.customOrderDao().saveOrder(
                CustomOrderEntity(listId = list.id, order = newOrder.joinToString(","))
            )
        }

        switchActiveList(list.id)
    }

    /** RFC-4180-compliant CSV line splitter that handles quoted fields. */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when {
                line[i] == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i += 2
                }
                line[i] == '"' -> { inQuotes = !inQuotes; i++ }
                line[i] == ',' && !inQuotes -> { fields.add(sb.toString()); sb.clear(); i++ }
                else -> { sb.append(line[i]); i++ }
            }
        }
        fields.add(sb.toString())
        return fields
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val db: TrackerDatabase, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CounterViewModel(db, context) as T
    }
}
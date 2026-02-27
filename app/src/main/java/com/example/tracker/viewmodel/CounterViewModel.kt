package com.example.tracker.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tracker.data.Counter
import com.example.tracker.data.CounterGroup
import com.example.tracker.data.CustomOrderEntity
import com.example.tracker.data.TrackerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

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

class CounterViewModel(private val db: TrackerDatabase) : ViewModel() {
    private val _counters = mutableStateListOf<Counter>()
    val counters: List<Counter> = _counters

    private val _groups = mutableStateListOf<CounterGroup>()
    val groups: List<CounterGroup> = _groups

    /**
     * Ordered display list for CUSTOM sort. Holds group IDs and counter IDs so
     * the custom order persists even when groups/counters are renamed or values change.
     * Each entry is either "g:<groupId>" or "c:<counterId>".
     */
    private val _customOrder = mutableStateListOf<String>()

    private var counterSequence = 0
    private var groupSequence   = 0

    private val _sortOrder = mutableStateOf(SortOrder.CUSTOM)
    val sortOrder = _sortOrder

    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }

    /** Key of the most recently added item ("g:<id>" or "c:<id>"). Null after consumed. */
    private val _lastAddedKey = mutableStateOf<String?>(null)
    val lastAddedKey = _lastAddedKey

    /** Call this after the UI has scrolled to the new item. */
    fun consumeLastAddedKey() { _lastAddedKey.value = null }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private fun saveOrder() {
        viewModelScope.launch {
            db.customOrderDao().saveOrder(CustomOrderEntity(order = _customOrder.joinToString(",")))
        }
    }

    // ── Initialisation — load from DB (or seed defaults if empty) ─────────────

    init {
        viewModelScope.launch {
            val savedGroups   = db.counterGroupDao().getAllFlow().first()
            val savedCounters = db.counterDao().getAllFlow().first()
            val savedOrder    = db.customOrderDao().getOrderList().firstOrNull()

            if (savedCounters.isEmpty() && savedGroups.isEmpty()) {
                // First launch — seed three default counters
                repeat(3) { addCounter() }
            } else {
                _groups.addAll(savedGroups)
                _counters.addAll(savedCounters)
                groupSequence   = savedGroups.size
                counterSequence = savedCounters.size

                if (savedOrder != null && savedOrder.isNotBlank()) {
                    _customOrder.addAll(savedOrder.split(",").filter { it.isNotBlank() })
                }
                rebuildCustomOrder()
            }
        }
    }

    // ── Custom-order helpers ──────────────────────────────────────────────────

    private fun rebuildCustomOrder() {
        val existing = _customOrder.toSet()
        // Add any new groups/ungrouped counters not yet in the order
        _groups.forEach { g ->
            val key = "g:${g.id}"
            if (key !in existing) _customOrder.add(key)
        }
        _counters.filter { it.groupId == null }.forEach { c ->
            val key = "c:${c.id}"
            if (key !in existing) _customOrder.add(key)
        }
        // Remove keys whose backing item no longer exists
        val validKeys = _groups.map { "g:${it.id}" }.toSet() +
                        _counters.filter { it.groupId == null }.map { "c:${it.id}" }.toSet()
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
        val groupItems  = _groups.map { DisplayItem.Group(it) }
        val counterItems = _counters.filter { it.groupId == null }
                                    .map { DisplayItem.UngroupedCounter(it) }
        val all: List<DisplayItem> = groupItems + counterItems

        return when (_sortOrder.value) {
            SortOrder.VALUE_HIGH_LOW -> all.sortedByDescending { item ->
                when (item) {
                    is DisplayItem.Group            -> _counters.filter { it.groupId == item.group.id }.sumOf { it.value }
                    is DisplayItem.UngroupedCounter -> item.counter.value
                }
            }
            SortOrder.VALUE_LOW_HIGH -> all.sortedBy { item ->
                when (item) {
                    is DisplayItem.Group            -> _counters.filter { it.groupId == item.group.id }.sumOf { it.value }
                    is DisplayItem.UngroupedCounter -> item.counter.value
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
                val groupMap   = _groups.associateBy   { "g:${it.id}" }
                val counterMap = _counters.filter { it.groupId == null }.associateBy { "c:${it.id}" }
                _customOrder.mapNotNull { key ->
                    groupMap[key]?.let   { DisplayItem.Group(it) }
                    ?: counterMap[key]?.let { DisplayItem.UngroupedCounter(it) }
                }
            }
        }
    }

    // ── Legacy helpers ────────────────────────────────────────────────────────
    fun getCountersInGroup(groupId: String): List<Counter> = _counters.filter { it.groupId == groupId }
    fun getUngroupedCounters(): List<Counter> = _counters.filter { it.groupId == null }

    // ── Counter CRUD ──────────────────────────────────────────────────────────

    fun addCounter(name: String? = null, groupId: String? = null) {
        counterSequence++
        val counter = Counter(
            id      = UUID.randomUUID().toString(),
            name    = name ?: "Counter $counterSequence",
            value   = 0,
            groupId = groupId
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
        val ids = _counters.map { it.id }.toSet()
        _counters.clear()
        _customOrder.removeAll { key -> ids.any { key == "c:$it" } }
        saveOrder()
        viewModelScope.launch { db.counterDao().deleteAll() }
    }

    fun incrementCounter(counterId: String) {
        val i = _counters.indexOfFirst { it.id == counterId }
        if (i == -1) return
        _counters[i] = _counters[i].copy(value = _counters[i].value + 1)
        viewModelScope.launch { db.counterDao().upsert(_counters[i]) }
    }

    fun decrementCounter(counterId: String) {
        val i = _counters.indexOfFirst { it.id == counterId }
        if (i == -1) return
        _counters[i] = _counters[i].copy(value = _counters[i].value - 1)
        viewModelScope.launch { db.counterDao().upsert(_counters[i]) }
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
        val paletteColors = listOf(
            0xFFE53935L, 0xFFF4511EL, 0xFFFFB300L, 0xFFFDD835L, 0xFF43A047L,
            0xFF00897BL, 0xFF1E88E5L, 0xFF3949ABL, 0xFF8E24AAL, 0xFFD81B60L,
        )
        val usedColors  = _groups.map { it.colorValue }.toSet()
        val chosenColor = colorValue
            ?: paletteColors.firstOrNull { it !in usedColors }
            ?: paletteColors[_groups.size % paletteColors.size]
        val group = CounterGroup(UUID.randomUUID().toString(), resolvedName, chosenColor)
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
            // Persist ungrouped counters
            _counters.filter { it.groupId == null }.forEach { db.counterDao().upsert(it) }
        }
    }

    fun updateGroupName(groupId: String, name: String) {
        val i = _groups.indexOfFirst { it.id == groupId }
        if (i == -1) return
        _groups[i] = _groups[i].copy(name = name)
        viewModelScope.launch { db.counterGroupDao().upsert(_groups[i]) }
    }

    fun updateGroupColor(groupId: String, colorValue: Long) {
        val i = _groups.indexOfFirst { it.id == groupId }
        if (i == -1) return
        _groups[i] = _groups[i].copy(colorValue = colorValue)
        viewModelScope.launch { db.counterGroupDao().upsert(_groups[i]) }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val db: TrackerDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CounterViewModel(db) as T
    }
}
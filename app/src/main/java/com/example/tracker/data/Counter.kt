package com.example.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counter_lists")
data class CounterList(
    @PrimaryKey val id: String = "",
    val name: String = "Default",
    val position: Int = 0
)

@Entity(tableName = "counters")
data class Counter(
    @PrimaryKey val id: String = "",
    val name: String = "Counter",
    val value: Int = 0,
    val groupId: String? = null,
    val color: Long? = null,
    val listId: String = ""
)

@Entity(tableName = "counter_groups")
data class CounterGroup(
    @PrimaryKey val id: String = "",
    val name: String = "Group",
    val colorValue: Long = 0xFF1E88E5L,
    val listId: String = ""
)

/** Stores the custom drag order as a comma-separated list of "g:<id>" / "c:<id>" keys, keyed by listId. */
@Entity(tableName = "custom_order")
data class CustomOrderEntity(
    @PrimaryKey val listId: String = "",  // one row per list
    val order: String = ""                // e.g. "g:abc,c:def,c:xyz"
)

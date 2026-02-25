package com.example.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counters")
data class Counter(
    @PrimaryKey val id: String = "",
    val name: String = "Counter",
    val value: Int = 0,
    val groupId: String? = null
)

@Entity(tableName = "counter_groups")
data class CounterGroup(
    @PrimaryKey val id: String = "",
    val name: String = "Group",
    val colorValue: Long = 0xFF1E88E5L
)

/** Stores the custom drag order as a comma-separated list of "g:<id>" / "c:<id>" keys. */
@Entity(tableName = "custom_order")
data class CustomOrderEntity(
    @PrimaryKey val rowId: Int = 0,   // always 0 — single-row table
    val order: String = ""            // e.g. "g:abc,c:def,c:xyz"
)


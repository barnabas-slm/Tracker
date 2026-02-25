package com.example.tracker.data

data class Counter(
    val id: String = "",
    val name: String = "Counter",
    val value: Int = 0,
    val groupId: String? = null
)

data class CounterGroup(
    val id: String = "",
    val name: String = "Group",
    val colorValue: Long = 0xFF1E88E5L
)

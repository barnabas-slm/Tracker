package com.example.tracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tracker.viewmodel.CounterViewModel
import com.example.tracker.viewmodel.SortOrder

@Composable
fun TrackerApp(viewModel: CounterViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onNavigateToAbout = { navController.navigate("about") }
            )
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CounterViewModel, onNavigateToAbout: () -> Unit) {
    var showMenu         by rememberSaveable { mutableStateOf(false) }
    var showSortMenu     by rememberSaveable { mutableStateOf(false) }
    var editingCounterId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingGroupId   by rememberSaveable { mutableStateOf<String?>(null) }

    // Per-group expanded state: true = expanded (default), false = collapsed
    val groupExpandedState = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Tracker", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.surface,
                    titleContentColor      = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Sort
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        val cur = viewModel.sortOrder.value
                        fun bold(o: SortOrder) = if (cur == o) FontWeight.Bold else FontWeight.Normal
                        DropdownMenuItem(text = { Text("Custom Order",       fontWeight = bold(SortOrder.CUSTOM))         }, onClick = { viewModel.setSortOrder(SortOrder.CUSTOM);         showSortMenu = false })
                        DropdownMenuItem(text = { Text("Value: High to Low", fontWeight = bold(SortOrder.VALUE_HIGH_LOW)) }, onClick = { viewModel.setSortOrder(SortOrder.VALUE_HIGH_LOW); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Value: Low to High", fontWeight = bold(SortOrder.VALUE_LOW_HIGH)) }, onClick = { viewModel.setSortOrder(SortOrder.VALUE_LOW_HIGH); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Alphabetical: A → Z",fontWeight = bold(SortOrder.ALPHA_AZ))      }, onClick = { viewModel.setSortOrder(SortOrder.ALPHA_AZ);        showSortMenu = false })
                        DropdownMenuItem(text = { Text("Alphabetical: Z → A",fontWeight = bold(SortOrder.ALPHA_ZA))      }, onClick = { viewModel.setSortOrder(SortOrder.ALPHA_ZA);        showSortMenu = false })
                    }
                    // Add counter
                    IconButton(onClick = { viewModel.addCounter() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Counter")
                    }
                    // Overflow
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Add Group") }, onClick = { showMenu = false; viewModel.addGroup() })
                        DropdownMenuItem(text = { Text("Collapse All") }, onClick = {
                            showMenu = false
                            viewModel.groups.forEach { groupExpandedState[it.id] = false }
                        })
                        DropdownMenuItem(text = { Text("Expand All") }, onClick = {
                            showMenu = false
                            viewModel.groups.forEach { groupExpandedState[it.id] = true }
                        })
                        DropdownMenuItem(
                            text = { Text("Delete All Counters", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; viewModel.removeAllCounters() }
                        )
                        DropdownMenuItem(text = { Text("About Tracker") }, onClick = { showMenu = false; onNavigateToAbout() })
                    }
                }
            )
        }
    ) { innerPadding ->
        CountersScreen(
            displayItems        = viewModel.sortedDisplayItems(),
            sortOrder           = viewModel.sortOrder.value,
            getInGroup          = { viewModel.getCountersInGroup(it) },
            onIncrement         = { viewModel.incrementCounter(it) },
            onDecrement         = { viewModel.decrementCounter(it) },
            onCounterClick      = { editingCounterId = it },
            onGroupTitleClick   = { editingGroupId = it },
            onReorder           = { from, to -> viewModel.reorderItems(from, to) },
            groupExpandedState  = groupExpandedState,
            modifier            = Modifier.padding(innerPadding),
            lastAddedKey        = viewModel.lastAddedKey.value,
            onLastAddedConsumed = { viewModel.consumeLastAddedKey() }
        )
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    editingCounterId?.let { cid ->
        val counter = viewModel.counters.find { it.id == cid } ?: run { editingCounterId = null; return@let }
        CounterSettingsDialog(
            counterName    = counter.name,
            counterValue   = counter.value,
            groups         = viewModel.groups.map { it.id to it.name },
            currentGroupId = counter.groupId,
            onDismiss      = { editingCounterId = null },
            onSave         = { newName, newValue, newGroupId ->
                viewModel.updateCounterName(cid, newName)
                viewModel.setCounterValue(cid, newValue)
                viewModel.assignCounterToGroup(cid, newGroupId)
                editingCounterId = null
            },
            onDelete = { viewModel.removeCounter(cid); editingCounterId = null }
        )
    }

    editingGroupId?.let { gid ->
        val group = viewModel.groups.find { it.id == gid } ?: run { editingGroupId = null; return@let }
        GroupSettingsDialog(
            groupName       = group.name,
            groupColorValue = group.colorValue,
            onDismiss       = { editingGroupId = null },
            onSave          = { newName, newColor ->
                viewModel.updateGroupName(gid, newName)
                viewModel.updateGroupColor(gid, newColor)
                editingGroupId = null
            },
            onDelete = { viewModel.removeGroup(gid); editingGroupId = null }
        )
    }
}




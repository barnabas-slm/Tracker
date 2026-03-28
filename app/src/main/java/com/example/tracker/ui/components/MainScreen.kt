package com.example.tracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import com.example.tracker.viewmodel.CounterViewModel
import java.io.File

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
    val context = LocalContext.current
    var showMenu         by rememberSaveable { mutableStateOf(false) }
    var showSortSheet    by rememberSaveable { mutableStateOf(false) }
    var editingCounterId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingGroupId   by rememberSaveable { mutableStateOf<String?>(null) }

    // Per-group expanded state: true = expanded (default), false = collapsed
    val groupExpandedState = remember { mutableStateMapOf<String, Boolean>() }

    fun shareCsv() {
        val csv = viewModel.buildCsvExport()
        val exportDir = File(context.cacheDir, "export").also { it.mkdirs() }
        val file = File(exportDir, "counters.csv").also { it.writeText(csv) }
        val uri = FileProvider.getUriForFile(context, "com.example.tracker.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Tracker counters export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export CSV"))
    }

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
                    // Sort — opens bottom sheet
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Sort") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Sort")
                        }
                    }
                    // Add group
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add Group") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { viewModel.addGroup() }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Group")
                        }
                    }
                    // Add counter
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add Counter") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { viewModel.addCounter() }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Counter")
                        }
                    }
                    // Overflow
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("More options") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Collapse All") }, onClick = {
                            showMenu = false
                            viewModel.groups.forEach { groupExpandedState[it.id] = false }
                        })
                        DropdownMenuItem(text = { Text("Expand All") }, onClick = {
                            showMenu = false
                            viewModel.groups.forEach { groupExpandedState[it.id] = true }
                        })
                        DropdownMenuItem(text = { Text("Export CSV") }, onClick = { showMenu = false; shareCsv() })
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

    // ── Sort bottom sheet ─────────────────────────────────────────────────────

    if (showSortSheet) {
        SortBottomSheet(
            currentSortOrder  = viewModel.sortOrder.value,
            onSortOrderChange = { viewModel.setSortOrder(it) },
            onDismiss         = { showSortSheet = false }
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
            currentColor   = counter.color,
            onDismiss      = { editingCounterId = null },
            onSave         = { newName, newValue, newGroupId, newColor ->
                viewModel.updateCounterName(cid, newName)
                viewModel.setCounterValue(cid, newValue)
                viewModel.assignCounterToGroup(cid, newGroupId)
                viewModel.updateCounterColor(cid, newColor)
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


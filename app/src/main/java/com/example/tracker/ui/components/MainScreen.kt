package com.example.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.tracker.viewmodel.CounterViewModel
import com.example.tracker.data.GroupMetric
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
    val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showMenu         by rememberSaveable { mutableStateOf(false) }
    var showFabMenu      by rememberSaveable { mutableStateOf(false) }
    var showSortSheet    by rememberSaveable { mutableStateOf(false) }
    var editingCounterId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingGroupId   by rememberSaveable { mutableStateOf<String?>(null) }
    var editingListId    by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDeleteAllGroups by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteAllCounters by rememberSaveable { mutableStateOf(false) }

    // Per-group expanded state: true = expanded (default), false = collapsed
    val groupExpandedState = remember { mutableStateMapOf<String, Boolean>() }

    val lists        = viewModel.lists
    val activeListId = viewModel.activeListId.value
    val selectedTabIndex = lists.indexOfFirst { it.id == activeListId }.coerceAtLeast(0)

    fun shareCsv() {
        val listName = viewModel.lists.find { it.id == activeListId }?.name ?: "counters"
        val safeFileName = listName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .ifBlank { "counters" }
        val csv = viewModel.buildCsvExport()
        val exportDir = File(context.cacheDir, "export").also { it.mkdirs() }
        val file = File(exportDir, "$safeFileName.csv").also { it.writeText(csv) }
        val uri = FileProvider.getUriForFile(context, "com.example.tracker.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Tracker counters export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export CSV"))
    }

    // Launcher for importing a CSV file via the system document picker (SAF).
    // No READ_EXTERNAL_STORAGE permission is required; SAF grants temporary access.
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            // Resolve a human-readable display name to use as the new list name.
            val displayName: String = run {
                var name: String? = null
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) name = cursor.getString(idx)
                    }
                }
                name ?: uri.lastPathSegment ?: "Imported List"
            }
            val listName = displayName
                .removeSuffix(".csv")
                .removeSuffix(".CSV")
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.readText()
                ?: return@rememberLauncherForActivityResult
            viewModel.importFromCsv(content, listName)
        } catch (_: Exception) { /* ignore read errors silently */ }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) {
                    Icon(Icons.Default.Add, contentDescription = "Add items")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Counter") },
                        onClick = {
                            showFabMenu = false
                            viewModel.addCounter()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add Group") },
                        onClick = {
                            showFabMenu = false
                            viewModel.addGroup()
                        }
                    )
                }
            }
        },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Tracker", fontWeight = FontWeight.Bold) },
                    scrollBehavior = topAppBarScrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor         = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor      = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        // Sort — opens bottom sheet
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Below
                            ),
                            tooltip = { PlainTooltip { Text("Sort") } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { showSortSheet = true }) {
                                Icon(Icons.Default.SwapVert, contentDescription = "Sort")
                            }
                        }
                        // Overflow
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Below
                            ),
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
                            DropdownMenuItem(
                                text = { Text("Import from CSV") },
                                onClick = {
                                    showMenu = false
                                    csvImportLauncher.launch(
                                        arrayOf("text/csv", "text/comma-separated-values")
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete All Groups", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    confirmDeleteAllGroups = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete All Counters", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    confirmDeleteAllCounters = true
                                }
                            )
                            DropdownMenuItem(text = { Text("About Tracker") }, onClick = { showMenu = false; onNavigateToAbout() })
                        }
                    }
                )

                // ── List tabs ─────────────────────────────────────────────────
                // Guard: only render the tab row once lists have loaded from DB.
                // ScrollableTabRow crashes with IndexOutOfBoundsException when
                // selectedTabIndex >= 0 but the tab list is still empty.
                if (lists.isNotEmpty()) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier         = Modifier.fillMaxWidth(),
                        edgePadding      = 0.dp,
                        containerColor   = MaterialTheme.colorScheme.background,
                        contentColor     = MaterialTheme.colorScheme.onBackground,
                        divider          = {},
                        indicator = {
                            if (selectedTabIndex < lists.size) {
                                TabRowDefaults.PrimaryIndicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(
                                            selectedTabIndex = selectedTabIndex,
                                            matchContentSize = false
                                        )
                                        .height(3.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        // ── Regular list tabs ──────────────────────────────────
                        lists.forEach { list ->
                            val isActive = list.id == activeListId
                            Tab(
                                selected = isActive,
                                // Tap unselected tab → switch list.
                                // Tap the already-selected tab → open list settings.
                                onClick = {
                                    if (isActive) editingListId = list.id
                                    else viewModel.switchActiveList(list.id)
                                },
                                text = {
                                    Text(
                                        text     = list.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }

                        // ── "New List" tab (always unselected, acts as a button) ─
                        Tab(
                            selected = false,
                            onClick  = { viewModel.addList() },
                            text = {
                                Row(
                                    verticalAlignment    = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier           = Modifier.size(16.dp)
                                    )
                                    Text("New List")
                                }
                            }
                        )
                    }
                }
            }
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

    ConfirmDeleteOverlay(
        visible = confirmDeleteAllGroups,
        onDismiss = { confirmDeleteAllGroups = false },
        onConfirm = {
            viewModel.groups
                .filter { it.listId == activeListId }
                .forEach { groupExpandedState.remove(it.id) }
            viewModel.removeAllGroups()
        }
    )

    ConfirmDeleteOverlay(
        visible = confirmDeleteAllCounters,
        onDismiss = { confirmDeleteAllCounters = false },
        onConfirm = { viewModel.removeAllCounters() }
    )

    } // end Box

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
            groups         = viewModel.groups
                                .filter { it.listId == viewModel.activeListId.value }
                                .map { it.id to it.name },
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
            groupColorValue = group.colorValue.takeIf { it != 0L },
            groupMetric     = group.metric,
            onDismiss       = { editingGroupId = null },
            onSave          = { newName, newColor, newMetric ->
                viewModel.updateGroupName(gid, newName)
                viewModel.updateGroupColor(gid, newColor)
                viewModel.updateGroupMetric(gid, newMetric)
                editingGroupId = null
            },
            onDelete = { viewModel.removeGroup(gid); editingGroupId = null }
        )
    }

    editingListId?.let { lid ->
        val list = viewModel.lists.find { it.id == lid } ?: run { editingListId = null; return@let }
        ListSettingsDialog(
            listName    = list.name,
            isOnlyList  = viewModel.lists.size <= 1,
            onDismiss   = { editingListId = null },
            onExportCsv = { shareCsv() },
            onSave      = { newName ->
                viewModel.renameList(lid, newName)
                editingListId = null
            },
            onDelete    = {
                viewModel.removeList(lid)
                editingListId = null
            }
        )
    }

}


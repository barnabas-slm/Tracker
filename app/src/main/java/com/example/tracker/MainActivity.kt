package com.example.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tracker.data.Counter
import com.example.tracker.data.CounterGroup
import com.example.tracker.data.TrackerDatabase
import com.example.tracker.ui.components.CounterCard
import com.example.tracker.ui.components.CounterSettingsDialog
import com.example.tracker.ui.components.GroupSettingsDialog
import com.example.tracker.ui.theme.TrackerTheme
import com.example.tracker.viewmodel.CounterViewModel
import com.example.tracker.viewmodel.DisplayItem
import com.example.tracker.viewmodel.SortOrder
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    private val counterViewModel: CounterViewModel by viewModels {
        CounterViewModel.Factory(TrackerDatabase.getInstance(applicationContext))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TrackerTheme { TrackerApp(viewModel = counterViewModel) } }
    }
}

// ── Root composable ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp(viewModel: CounterViewModel) {
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
                    }
                }
            )
        }
    ) { innerPadding ->
        CountersScreen(
            displayItems       = viewModel.sortedDisplayItems(),
            sortOrder          = viewModel.sortOrder.value,
            getInGroup         = { viewModel.getCountersInGroup(it) },
            onIncrement        = { viewModel.incrementCounter(it) },
            onDecrement        = { viewModel.decrementCounter(it) },
            onCounterClick     = { editingCounterId = it },
            onGroupTitleClick  = { editingGroupId = it },
            onReorder          = { from, to -> viewModel.reorderItems(from, to) },
            groupExpandedState = groupExpandedState,
            modifier           = Modifier.padding(innerPadding)
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

// ── Main screen ───────────────────────────────────────────────────────────────
@Composable
fun CountersScreen(
    displayItems: List<DisplayItem>,
    sortOrder: SortOrder,
    getInGroup: (String) -> List<Counter>,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onCounterClick: (String) -> Unit,
    onGroupTitleClick: (String) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    groupExpandedState: MutableMap<String, Boolean>,
    modifier: Modifier = Modifier
) {
    if (displayItems.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No counters yet.", style = MaterialTheme.typography.bodyLarge)
            Text("Tap + to add one.", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val lazyListState    = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyColumn(
        state   = lazyListState,
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(displayItems, key = { item ->
            when (item) {
                is DisplayItem.Group            -> "g:${item.group.id}"
                is DisplayItem.UngroupedCounter -> "c:${item.counter.id}"
            }
        }) { item ->
            ReorderableItem(reorderableState, key = when (item) {
                is DisplayItem.Group            -> "g:${item.group.id}"
                is DisplayItem.UngroupedCounter -> "c:${item.counter.id}"
            }) { isDragging ->
                val dragMod = if (sortOrder == SortOrder.CUSTOM)
                    Modifier.longPressDraggableHandle() else Modifier

                when (item) {
                    is DisplayItem.Group -> GroupCard(
                        group              = item.group,
                        counters           = getInGroup(item.group.id),
                        onTitleClick       = { onGroupTitleClick(item.group.id) },
                        onIncrement        = onIncrement,
                        onDecrement        = onDecrement,
                        onCounterClick     = onCounterClick,
                        isDragging         = isDragging,
                        dragModifier       = dragMod,
                        groupExpandedState = groupExpandedState
                    )
                    is DisplayItem.UngroupedCounter -> UngroupedCounterCard(
                        counter      = item.counter,
                        onIncrement  = { onIncrement(item.counter.id) },
                        onDecrement  = { onDecrement(item.counter.id) },
                        onTitleClick = { onCounterClick(item.counter.id) },
                        isDragging   = isDragging,
                        dragModifier = dragMod
                    )
                }
            }
        }
    }
}

// ── Ungrouped counter card (individual, light gray) ───────────────────────────
@Composable
fun UngroupedCounterCard(
    counter: Counter,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onTitleClick: () -> Unit,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp)
    ) {
        // The drag handle covers the whole card row — long-press anywhere on it to drag
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(dragModifier)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CounterCard(
                counter      = counter,
                onIncrement  = onIncrement,
                onDecrement  = onDecrement,
                onTitleClick = onTitleClick,
                modifier     = Modifier.fillMaxWidth(),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Group card ────────────────────────────────────────────────────────────────
@Composable
fun GroupCard(
    group: CounterGroup,
    counters: List<Counter>,
    onTitleClick: () -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onCounterClick: (String) -> Unit,
    groupExpandedState: MutableMap<String, Boolean>,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    // Default to expanded; state is owned by parent so Collapse/Expand All works
    val expanded = groupExpandedState.getOrDefault(group.id, true)
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrowRotation"
    )

    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = Color(group.colorValue)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize(animationSpec = tween(durationMillis = 300)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(dragModifier)
                    .padding(bottom = if (expanded) 4.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = group.name,
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    modifier   = Modifier.weight(1f).clickable { onTitleClick() }
                )
                Text(
                    text       = counters.sumOf { it.value }.toString(),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White.copy(alpha = 0.85f)
                )
                IconButton(onClick = { groupExpandedState[group.id] = !expanded }) {
                    Icon(
                        imageVector        = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint               = Color.White,
                        modifier           = Modifier.rotate(arrowRotation)
                    )
                }
            }
            if (expanded) {
                if (counters.isEmpty()) {
                    Text(
                        "No counters in this group",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    counters.forEach { counter ->
                        CounterCard(
                            counter      = counter,
                            onIncrement  = { onIncrement(counter.id) },
                            onDecrement  = { onDecrement(counter.id) },
                            onTitleClick = { onCounterClick(counter.id) }
                        )
                    }
                }
            }
        }
    }
}

// Preview omitted — CounterViewModel requires Application context

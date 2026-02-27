package com.example.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tracker.data.Counter
import com.example.tracker.viewmodel.DisplayItem
import com.example.tracker.viewmodel.SortOrder
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    modifier: Modifier = Modifier,
    lastAddedKey: String? = null,
    onLastAddedConsumed: () -> Unit = {}
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

    // Scroll to newly added item
    LaunchedEffect(lastAddedKey) {
        if (lastAddedKey != null) {
            val index = displayItems.indexOfFirst { item ->
                when (item) {
                    is DisplayItem.Group            -> "g:${item.group.id}" == lastAddedKey
                    is DisplayItem.UngroupedCounter -> "c:${item.counter.id}" == lastAddedKey
                }
            }
            if (index != -1) {
                lazyListState.animateScrollToItem(index)
            }
            onLastAddedConsumed()
        }
    }

    LazyColumn(
        state    = lazyListState,
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
                        groupExpandedState = groupExpandedState,
                        isDragging         = isDragging,
                        dragModifier       = dragMod
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

@Preview
@Composable
fun CountersScreenPreview() {
    CountersScreen(
        displayItems = listOf(
            DisplayItem.Group(
                group = com.example.tracker.data.CounterGroup(id = "g1", name = "Group 1", colorValue = 0xFFBB86FC)
            ),
            DisplayItem.UngroupedCounter(
                counter = Counter(id = "c1", name = "Ungrouped Counter", value = 5)
            )
        ),
        sortOrder = SortOrder.CUSTOM,
        getInGroup = { emptyList() },
        onIncrement = {},
        onDecrement = {},
        onCounterClick = {},
        onGroupTitleClick = {},
        onReorder = { _, _ -> },
        groupExpandedState = mutableMapOf("g1" to true)
    )
}


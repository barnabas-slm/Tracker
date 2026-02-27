package com.example.tracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tracker.data.Counter
import com.example.tracker.data.CounterGroup

@Composable
fun GroupCard(
    group: CounterGroup,
    counters: List<Counter>,
    onTitleClick: () -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onCounterClick: (String) -> Unit,
    groupExpandedState: MutableMap<String, Boolean>,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier
) {
    // Default to expanded; state is owned by parent so Collapse/Expand All works
    val expanded = groupExpandedState.getOrDefault(group.id, true)
    val arrowRotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label         = "arrowRotation"
    )

    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = Color(group.colorValue)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
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

@Preview
@Composable
fun GroupCardPreview() {
    GroupCard(
        group              = CounterGroup(id = "g1", name = "Sample Group", colorValue = 0xFF6200EE),
        counters           = listOf(
            Counter(id = "c1", name = "Counter A", value = 5),
            Counter(id = "c2", name = "Counter B", value = 10),
            Counter(id = "c3", name = "Counter C", value = 3)
        ),
        onTitleClick       = {},
        onIncrement        = {},
        onDecrement        = {},
        onCounterClick     = {},
        groupExpandedState = mutableMapOf("g1" to true)
    )
}



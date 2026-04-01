package com.example.tracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tracker.data.Counter

@Composable
fun UngroupedCounterCard(
    counter: Counter,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
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
                modifier     = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun UngroupedCounterCardPreview() {
    UngroupedCounterCard(
        counter      = Counter(id = "1", name = "Ungrouped Counter", value = 5, color = 0xFF90CAF9L),
        onIncrement  = {},
        onDecrement  = {},
        onTitleClick = {}
    )
}

package com.example.tracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tracker.data.Counter

private fun Color.contrastTextColor(): Color {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return if (luminance > 0.6f) Color(0xFF212121) else Color.White
}

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
    val backgroundColor = counter.color?.let { Color(it) }
    val contentColor = backgroundColor?.contrastTextColor()

    val content: @Composable () -> Unit = {
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
                contentColor = contentColor ?: androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (backgroundColor != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
        ) { content() }
    } else {
        OutlinedCard(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
        ) { content() }
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

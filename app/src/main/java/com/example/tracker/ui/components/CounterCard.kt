package com.example.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tracker.data.Counter

@Composable
fun CounterCard(
    counter: Counter,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = counter.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier
                .weight(1f)
                .clickable { onTitleClick() }
        )
        FilledTonalIconButton(
            onClick = onDecrement,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = contentColor.copy(alpha = 0.20f),
                contentColor = contentColor
            )
        ) {
            Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
        Text(
            text = counter.value.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(72.dp)
        )
        FilledTonalIconButton(
            onClick = onIncrement,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = contentColor.copy(alpha = 0.20f),
                contentColor = contentColor
            )
        ) {
            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Preview
@Composable
fun CounterCardPreview() {
    CounterCard(
        counter = Counter(id = "1", name = "Sample Counter", value = 42),
        onIncrement = {},
        onDecrement = {},
        onTitleClick = {}
    )
}
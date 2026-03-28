package com.example.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.tracker.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Custom order ──────────────────────────────────────────────────
            SortOptionRow(
                label = "Custom order",
                isSelected = currentSortOrder == SortOrder.CUSTOM,
                directionIcon = null,
                onClick = { onSortOrderChange(SortOrder.CUSTOM); onDismiss() }
            )

            // ── Value ─────────────────────────────────────────────────────────
            val valueSelected =
                currentSortOrder == SortOrder.VALUE_HIGH_LOW ||
                currentSortOrder == SortOrder.VALUE_LOW_HIGH
            SortOptionRow(
                label = "Value",
                isSelected = valueSelected,
                directionIcon = when (currentSortOrder) {
                    SortOrder.VALUE_HIGH_LOW -> Icons.Default.ArrowDownward
                    SortOrder.VALUE_LOW_HIGH -> Icons.Default.ArrowUpward
                    else -> null
                },
                onClick = {
                    onSortOrderChange(
                        when (currentSortOrder) {
                            SortOrder.VALUE_HIGH_LOW -> SortOrder.VALUE_LOW_HIGH
                            SortOrder.VALUE_LOW_HIGH -> SortOrder.VALUE_HIGH_LOW
                            else -> SortOrder.VALUE_HIGH_LOW
                        }
                    )
                    onDismiss()
                }
            )

            // ── Alphabetical ──────────────────────────────────────────────────
            val alphaSelected =
                currentSortOrder == SortOrder.ALPHA_AZ ||
                currentSortOrder == SortOrder.ALPHA_ZA
            SortOptionRow(
                label = "Alphabetical",
                isSelected = alphaSelected,
                directionIcon = when (currentSortOrder) {
                    SortOrder.ALPHA_AZ -> Icons.Default.ArrowUpward
                    SortOrder.ALPHA_ZA -> Icons.Default.ArrowDownward
                    else -> null
                },
                onClick = {
                    onSortOrderChange(
                        when (currentSortOrder) {
                            SortOrder.ALPHA_AZ -> SortOrder.ALPHA_ZA
                            SortOrder.ALPHA_ZA -> SortOrder.ALPHA_AZ
                            else -> SortOrder.ALPHA_AZ
                        }
                    )
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortOptionRow(
    label: String,
    isSelected: Boolean,
    directionIcon: ImageVector?,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading checkmark (or empty space to keep alignment)
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )

        // Direction indicator (only shown when this option is active)
        if (isSelected && directionIcon != null) {
            Icon(
                imageVector = directionIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

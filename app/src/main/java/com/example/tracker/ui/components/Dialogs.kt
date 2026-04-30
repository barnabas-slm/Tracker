package com.example.tracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tracker.data.GroupMetric

// ── Shared pastel colour palette ──────────────────────────────────────────────
val colorOptions: List<Pair<String, Long>> = listOf(
    "Blush"      to 0xFFF48FB1L,
    "Rose"       to 0xFFEF9A9AL,
    "Peach"      to 0xFFFFCC80L,
    "Butter"     to 0xFFFFE082L,
    "Lemon"      to 0xFFFFF59DL,
    "Mint"       to 0xFFA5D6A7L,
    "Aqua"       to 0xFF80CBC4L,
    "Sky"        to 0xFF90CAF9L,
    "Periwinkle" to 0xFF9FA8DAL,
    "Lavender"   to 0xFFCE93D8L,
)

// ── Reusable animated delete confirmation overlay ─────────────────────────────
@Composable
fun ConfirmDeleteOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(160)),
        exit  = fadeOut(animationSpec = tween(120))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .animateEnterExit(
                        enter = scaleIn(animationSpec = tween(180), initialScale = 0.96f),
                        exit  = scaleOut(animationSpec = tween(140), targetScale = 0.98f)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Are you sure?",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Start
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onDismiss() }) { Text("Cancel") }
                        TextButton(onClick = {
                            onDismiss()
                            onConfirm()
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// ── Add counter ───────────────────────────────────────────────────────────────
@Composable
fun AddCounterDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Counter") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Counter Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onAdd(name.trim()) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Add group ─────────────────────────────────────────────────────────────────
@Composable
fun AddGroupDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Group") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onAdd(name.trim()) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Counter settings dialog ───────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CounterSettingsDialog(
    counterName: String,
    counterValue: Int,
    groups: List<Pair<String, String>>,
    currentGroupId: String?,
    currentColor: Long?,
    onDismiss: () -> Unit,
    onSave: (newName: String, newValue: Int, newGroupId: String?, newColor: Long?) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var name  by remember { mutableStateOf(counterName) }
    var value by remember { mutableStateOf(counterValue.toString()) }
    var selectedGroupId      by remember { mutableStateOf(currentGroupId) }
    var selectedColor        by remember { mutableStateOf(currentColor) }
    var showCustomPicker     by remember { mutableStateOf(false) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { Text("Counter Settings") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        SettingsActionIconButton(
                            tooltip = "Duplicate Counter",
                            onClick = onDuplicate,
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Duplicate Counter"
                        )
                        SettingsActionIconButton(
                            tooltip = "Delete Counter",
                            onClick = onDelete,
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Counter"
                        )
                    }
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onSave(
                                name.trim().ifBlank { counterName },
                                value.toIntOrNull() ?: counterValue,
                                selectedGroupId,
                                selectedColor
                            )
                        }
                    ) { Text("Save") }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Value") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                val selectedGroupName = groups.find { it.first == selectedGroupId }?.second ?: "None"
                ExposedDropdownMenuBox(
                    expanded = groupDropdownExpanded,
                    onExpandedChange = { groupDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedGroupName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { selectedGroupId = null; groupDropdownExpanded = false }
                        )
                        groups.forEach { (id, gName) ->
                            DropdownMenuItem(
                                text = { Text(gName) },
                                onClick = { selectedGroupId = id; groupDropdownExpanded = false }
                            )
                        }
                    }
                }

                // Color section — only visible for ungrouped counters
                if (selectedGroupId == null) {
                    val isPaletteColor = colorOptions.any { it.second == selectedColor }
                    Text("Color", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        NoColorSwatch(selected = selectedColor == null && !showCustomPicker) {
                            selectedColor = null
                            showCustomPicker = false
                        }
                        colorOptions.forEach { (_, cv) ->
                            ColorSwatch(cv, selected = cv == selectedColor && !showCustomPicker) {
                                selectedColor = cv
                                showCustomPicker = false
                            }
                        }
                        // Custom swatch
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showCustomPicker || (selectedColor != null && !isPaletteColor))
                                        Color(selectedColor ?: 0xFFF48FB1L)
                                    else Color(0xFFCCCCCC)
                                )
                                .then(
                                    if (showCustomPicker || (selectedColor != null && !isPaletteColor))
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable { showCustomPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = "Custom color",
                                tint = Color.White
                            )
                        }
                    }
                    if (showCustomPicker) {
                        HsvColorPicker(
                            initialColor = Color(selectedColor ?: 0xFFF48FB1L),
                            onColorChanged = { selectedColor = it.toArgb().toLong() and 0xFFFFFFFFL }
                        )
                    }
                }
            }
        }
    }
}

// ── Group settings dialog ─────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsDialog(
    groupName: String,
    groupColorValue: Long?,
    groupMetric: GroupMetric = GroupMetric.SUM,
    onDismiss: () -> Unit,
    onSave: (newName: String, newColor: Long?, newMetric: GroupMetric) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var name  by remember { mutableStateOf(groupName) }
    var color by remember { mutableStateOf(groupColorValue) }
    var metric by remember { mutableStateOf(groupMetric) }
    var showCustomPicker by remember { mutableStateOf(false) }
    var metricDropdownExpanded by remember { mutableStateOf(false) }

    // Pre-compute whether current color is a palette color
    val isPaletteColor = color != null && colorOptions.any { it.second == color }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { Text("Group Settings") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        SettingsActionIconButton(
                            tooltip = "Duplicate Group",
                            onClick = onDuplicate,
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Duplicate Group"
                        )
                        SettingsActionIconButton(
                            tooltip = "Delete Group",
                            onClick = onDelete,
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Group"
                        )
                    }
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onSave(name.trim().ifBlank { groupName }, color, metric) }
                    ) { Text("Save") }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Metric dropdown
                ExposedDropdownMenuBox(
                    expanded = metricDropdownExpanded,
                    onExpandedChange = { metricDropdownExpanded = !metricDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = metric.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        label = { Text("Metric") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = metricDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = metricDropdownExpanded,
                        onDismissRequest = { metricDropdownExpanded = false }
                    ) {
                        GroupMetric.values().forEach { metricOption ->
                            DropdownMenuItem(
                                text = { Text(metricOption.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    metric = metricOption
                                    metricDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelLarge)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    NoColorSwatch(selected = color == null && !showCustomPicker) {
                        color = null
                        showCustomPicker = false
                    }
                    // Palette swatches
                    colorOptions.forEach { (_, cv) ->
                        ColorSwatch(cv, selected = cv == color && !showCustomPicker) {
                            color = cv
                            showCustomPicker = false
                        }
                    }
                    // Custom swatch — shows a rainbow circle; highlights when custom color is active
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (showCustomPicker || (color != null && !isPaletteColor)) Color(color ?: 0xFFF48FB1L)
                                else Color(0xFFCCCCCC)
                            )
                            .then(
                                if (showCustomPicker || (color != null && !isPaletteColor))
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable { showCustomPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Custom color",
                            tint = Color.White
                        )
                    }
                }

                // Inline HSV picker shown below swatches when custom is active
                if (showCustomPicker) {
                    HsvColorPicker(
                        initialColor = Color(color ?: 0xFFF48FB1L),
                        onColorChanged = { color = it.toArgb().toLong() and 0xFFFFFFFFL }
                    )
                }
            }
        }
    }
}

// ── List settings dialog ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSettingsDialog(
    listName: String,
    isOnlyList: Boolean,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onSave: (newName: String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(listName) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { Text("List Settings") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        SettingsActionIconButton(
                            tooltip = "Duplicate List",
                            onClick = onDuplicate,
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Duplicate List"
                        )
                        if (!isOnlyList) {
                            SettingsActionIconButton(
                                tooltip = "Delete List",
                                onClick = { showDeleteConfirm = true },
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete List"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onSave(name.trim().ifBlank { listName }) }
                    ) { Text("Save") }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TextButton(
                    onClick = { onExportCsv(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export CSV")
                }
            }
        }

        ConfirmDeleteOverlay(
            visible = showDeleteConfirm,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = onDelete
        )
    }
}

// ── HSV colour picker ─────────────────────────────────────────────────────────
@Composable
fun HsvColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
) {
    // Decompose initial color to HSV
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            (initialColor.alpha * 255).toInt(),
            (initialColor.red   * 255).toInt(),
            (initialColor.green * 255).toInt(),
            (initialColor.blue  * 255).toInt(),
        ), hsv
    )
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    fun currentColor(): Color {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        return Color(argb)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Preview strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(currentColor())
        )
        // Hue
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("H", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(16.dp))
            Slider(value = hue, onValueChange = { hue = it; onColorChanged(currentColor()) },
                valueRange = 0f..360f, modifier = Modifier.weight(1f))
        }
        // Saturation
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("S", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(16.dp))
            Slider(value = sat, onValueChange = { sat = it; onColorChanged(currentColor()) },
                valueRange = 0f..1f, modifier = Modifier.weight(1f))
        }
        // Value/brightness
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("V", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(16.dp))
            Slider(value = value, onValueChange = { value = it; onColorChanged(currentColor()) },
                valueRange = 0f..1f, modifier = Modifier.weight(1f))
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────
@Composable
fun GroupChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
             else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ColorSwatch(colorValue: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(colorValue))
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable { onClick() }
    )
}

@Composable
private fun NoColorSwatch(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.FormatColorReset,
            contentDescription = "No color",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsActionIconButton(
    tooltip: String,
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above
        ),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = imageVector, contentDescription = contentDescription)
        }
    }
}

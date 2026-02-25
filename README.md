# Tracker App — Summary

> Last updated: February 24, 2026

---

## Overview

Tracker is an Android app built with **Jetpack Compose** and **Material Design 3** that lets you maintain a list of named counters, organise them into coloured groups, and sort or manually reorder everything.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM — `ViewModel` + Compose state |
| Drag-to-reorder | `sh.calvin.reorderable` 2.4.3 |
| Min SDK | 24 (Android 7.0) |
| Target / Compile SDK | 36 |

---

## Project Structure

```
app/src/main/java/com/example/tracker/
├── MainActivity.kt                   – Root composable, screen, all cards
├── data/
│   └── Counter.kt                    – Counter + CounterGroup data classes
├── viewmodel/
│   └── CounterViewModel.kt           – All state & business logic
└── ui/
    ├── components/
    │   ├── CounterCard.kt            – Single counter row (title, −, value, +)
    │   └── Dialogs.kt                – All dialog composables + HSV colour picker
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## Data Model

### `Counter`
| Field | Type | Description |
|---|---|---|
| `id` | `String` | UUID |
| `name` | `String` | Display name |
| `value` | `Int` | Current count |
| `groupId` | `String?` | ID of parent group, or `null` if ungrouped |

### `CounterGroup`
| Field | Type | Description |
|---|---|---|
| `id` | `String` | UUID |
| `name` | `String` | Display name |
| `colorValue` | `Long` | ARGB card background colour |

### `DisplayItem` (sealed class)
The unified display list that drives the main screen. Each item is one of:
- `DisplayItem.Group(group)` — a group card containing its counters
- `DisplayItem.UngroupedCounter(counter)` — a single counter as its own card

---

## Features

### Counters
- **Add** — tap **+** in the top bar; a counter is created instantly with an auto-incremented name (`Counter 1`, `Counter 2`, …)
- **Increment / Decrement** — `−` and `+` buttons on each card
- **Fixed-width value display** — centred in a `72dp` box, comfortable for up to 5 digits, at `22sp Bold`; the `−`/`+` buttons never shift regardless of digit count
- **Edit** — tap the counter's **title** to open *Counter Settings*:
  - Rename the counter
  - Set an exact value
  - Assign or move to a group (or ungroup it)
  - Delete the counter
- **Delete all** — available in the top-bar overflow (⋮) menu

### Groups
- **Add** — tap **Add Group** from the ⋮ overflow menu; a dialog asks for the group name
- **Auto colour** — new groups are assigned the first palette colour not already used by another group (cycles through the 10 palette colours if all are taken)
- **Group card** — displays the group name (bold, white, `titleLarge`) and the **sum of all counter values** in the group at the top-right
- **Edit** — tap the group's **title** to open *Group Settings*:
  - Rename the group
  - Pick a colour:
    - 10 vibrant **palette swatches** (Red, Orange, Amber, Yellow, Green, Teal, Blue, Indigo, Purple, Pink)
    - A **✎ custom swatch** that opens an inline **HSV colour picker** (Hue / Saturation / Value sliders + live preview strip)
  - Delete the group (ungrouped counters from the deleted group are returned to the list)

### Ungrouped Counters
- Each ungrouped counter is rendered as its **own individual card** (light grey `#BDBDBD`)
- Ungrouped counters participate in **sorting and custom ordering** alongside group cards — there is no separate "Ungrouped" section

### Sorting
Tap the **⇅ sort button** in the top bar to choose:

| Option | Groups sorted by | Ungrouped counters sorted by |
|---|---|---|
| **Custom Order** *(default)* | User-defined drag order | User-defined drag order |
| **Value: High to Low** | Sum of counters ↓ | Individual value ↓ |
| **Value: Low to High** | Sum of counters ↑ | Individual value ↑ |
| **Alphabetical: A → Z** | Group name A→Z | Counter name A→Z |
| **Alphabetical: Z → A** | Group name Z→A | Counter name Z→A |

The currently active sort option is shown in **bold** in the menu.

### Custom Order & Drag-to-Reorder
- In **Custom Order** mode, **long-press** any card (group or ungrouped counter) to pick it up and drag it to a new position
- Dragging raises the card's elevation to `8dp` for visual feedback
- Switching to a different sort mode and back to Custom Order restores the last custom arrangement
- The custom order is maintained incrementally: adding/removing counters or groups, or moving a counter between groups, keeps the order consistent without a full rebuild

---

## UI Layout

### Top Bar (white background, black text/icons)
```
[ Tracker ]   [⇅ Sort]  [+ Add Counter]  [⋮ More]
                                              ├ Add Group
                                              └ Delete All Counters
```

### Main Screen (white background)
- Vertical `LazyColumn` of `DisplayItem` cards, `12dp` spacing
- **Group card** — coloured background, bold title + sum header, counter rows inside
- **Ungrouped counter card** — light grey, single counter row
- Each counter row: `[title (clickable)]  [−]  [value]  [+]`

---

## Dialogs

| Dialog | Opened by | Contents |
|---|---|---|
| **Add Group** | ⋮ → Add Group | Group name field |
| **Counter Settings** | Tapping counter title | Name, Value, Group picker (chips), Delete button |
| **Group Settings** | Tapping group title | Name, Colour picker (palette + HSV custom), Delete button |

---

## Build

```powershell
cd C:\Users\barna\AndroidStudioProjects\Tracker
.\gradlew assembleDebug      # build APK
.\gradlew installDebug       # install to connected device/emulator
```

**Status:** ✅ BUILD SUCCESSFUL


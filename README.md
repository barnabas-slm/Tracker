# Tracker App

An Android app built with Jetpack Compose and Material Design 3 for managing named counters organised into coloured groups.

---

## Key Features

### Counters
- **Add** — instantly creates a counter with an auto-incremented name (`Counter 1`, `Counter 2`, …)
- **Increment / Decrement** — `−` and `+` buttons on each card
- **Edit** — tap a counter's title to rename it, set an exact value, assign it to a group, or delete it
- **Delete all** — available in the ⋮ overflow menu

### Groups
- **Add** — creates a group instantly with an auto-incremented name (`Group 1`, `Group 2`, …) and an automatically assigned colour
- **Edit** — tap a group's title to rename it, pick a colour (10 palette swatches or a custom HSV picker), or delete it (counters are returned to the ungrouped list)
- **Summary** — each group card shows the sum of all its counter values

### Sorting & Reordering
- Sort by **Custom Order**, **Value (high→low / low→high)**, or **Alphabetical (A→Z / Z→A)**
- In Custom Order mode, **long-press** any card to drag it to a new position

### Night Mode
- Automatically follows the system dark/light theme

### Data Persistence
- All counters, groups, values, and custom sort order are automatically saved to the device
- Data persists between app sessions — close and reopen the app to resume where you left off



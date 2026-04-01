# Tracker App

An Android app built with Jetpack Compose and Material Design 3 for managing named counters organised into coloured groups across multiple lists.

---

## Key Features

### Multiple Lists
- **Tabs** — each list appears as a tab in a scrollable tab row at the top of the screen
- **Add** — tap the **+ New List** tab at the end to create a new list instantly
- **Switch** — tap any unselected tab to switch to that list
- **Settings** — tap the currently active tab to open List Settings (rename, export CSV, or delete)
- At least one list always exists; the delete button is hidden when only one list remains

### Counters
- **Add** — tap the **＋** FAB and choose **Add Counter**; the counter is named `Counter 1`, `Counter 2`, … automatically
- **Increment / Decrement** — `−` and `+` buttons on each card
- **Edit** — tap a counter's name to open Counter Settings: rename, set an exact value, assign to a group, or pick a card colour
- **Colour** — ungrouped counters can have an optional background colour chosen from a 10-swatch pastel palette or a custom HSV picker; the card text colour auto-adjusts for legibility
- **Delete** — delete a single counter from its settings dialog, or **Delete All Counters** from the ⋮ overflow menu

### Groups
- **Add** — tap the **＋** FAB and choose **Add Group**; the group is named `Group 1`, `Group 2`, … automatically
- **Colour** — pick a colour from a 10-swatch pastel palette or a custom HSV picker in Group Settings; card text auto-adjusts for contrast; groups with no colour use an outlined card style
- **Summary** — the group card header always shows the sum of all its counter values
- **Expand / Collapse** — tap the chevron on any group card to toggle it; use **Collapse All** / **Expand All** in the ⋮ overflow menu to act on all groups at once
- **Edit** — tap a group's name to open Group Settings: rename, change colour, or delete (counters are returned to the ungrouped list)
- **Delete All Groups** — available in the ⋮ overflow menu; all group counters are returned to the ungrouped list

### Sorting & Reordering
- Tap the **⇅** icon in the top bar to open the sort sheet
- Sort modes: **Custom Order**, **Value** (high → low or low → high), **Alphabetical** (A → Z or Z → A)
- Tapping an already-selected Value or Alphabetical mode toggles the direction
- In **Custom Order** mode, **long-press** any card to drag it to a new position; haptic feedback fires on drag start
- Value sort order updates at most every 3 seconds while tapping +/− so cards don't shuffle on every tap

### CSV Import & Export
- **Export** — open List Settings (tap the active tab) and tap **Export CSV**; the app shares a `<list-name>.csv` file via the system share sheet
- **Import** — tap ⋮ → **Import from CSV**; pick a `.csv` file with the system document picker (no storage permission required); a new list is created from the file using the filename as the list name and becomes the active list
- CSV format: `Group,Counter,Value` header row; ungrouped counters have an empty Group field

### ⋮ Overflow Menu
| Action | Effect |
|---|---|
| Collapse All | Collapses all group cards in the active list |
| Expand All | Expands all group cards in the active list |
| Import from CSV | Opens the system file picker to import a CSV as a new list |
| Delete All Groups | Removes all groups; counters return to the ungrouped list |
| Delete All Counters | Removes all counters in the active list |
| About Tracker | Opens the About screen |

### Night Mode
Automatically follows the system dark / light theme.

### Data Persistence
- Counters, groups, lists, values, colours, and custom sort order are saved automatically to a local Room database
- Sort mode preference is saved separately via DataStore
- Data persists across app restarts

# Tracker — AI Agent Guide

## Architecture Overview

Single-Activity MVVM Android app (Kotlin, Jetpack Compose, Material3).

```
MainActivity
  └─ TrackerTheme
       └─ TrackerApp (NavHost: "main" / "about")
             └─ MainScreen ─── PrimaryScrollableTabRow (one tab per CounterList + "New List")
                          └── CountersScreen
                                 ├─ GroupCard (+ CounterCard rows inside)
                                 └─ UngroupedCounterCard
```

**Layers:**
- `data/` — Room entities (`CounterList`, `Counter`, `CounterGroup`, `CustomOrderEntity`) all defined in `Counter.kt` (`Entities.kt` is empty); DAOs in `Daos.kt`; `TrackerDatabase` singleton
- `viewmodel/CounterViewModel.kt` — single ViewModel; in-memory `mutableStateListOf` is the source of truth; DB is write-through persistence only (loaded once at `init`)
- `ui/components/` — all Composables; `ui/theme/` — Material3 theme
- CSV export triggered from `ListSettingsDialog` (tap the active list tab → "Export CSV"); `CounterViewModel.buildCsvExport()` filters by `_activeListId`; Android sharing wired via `AndroidManifest.xml` `FileProvider` + `res/xml/file_paths.xml`
- CSV import via `CounterViewModel.importFromCsv(csvContent, listName)` (RFC-4180-compliant `parseCsvLine()` helper); triggered by "Import from CSV" in the `⋮` overflow menu using SAF (`ActivityResultContracts.OpenDocument()`) — no `READ_EXTERNAL_STORAGE` permission required; derives the new list name from the file's display name (strips `.csv` extension), creates a new `CounterList`, and calls `switchActiveList()`

## Key Data Model Patterns

### Entity conventions
- IDs are `UUID.randomUUID().toString()` strings
- Group color stored as `Long` ARGB hex literal (e.g. `0xFF1E88E5L`); construct `Color(colorValue)` in UI
- `Counter.color: Long?` — optional per-counter color (ungrouped counters only); defaults to `null` on `addCounter()` and is set via `CounterSettingsDialog`; shown as card background in `UngroupedCounterCard`; color picker hidden when the counter is assigned to a group
- `CustomOrderEntity` has **one row per list** (`@PrimaryKey val listId: String`); custom order serialized as comma-separated `"g:<uuid>"` / `"c:<uuid>"` tokens

### Multi-List Support
- `CounterList(id, name, position)` entity — lists are ordered by `position`; at least one list always exists (guard in `removeList()`)
- `Counter.listId` and `CounterGroup.listId` scope every entity to a list; `sortedDisplayItems()`, `getCountersInGroup()`, `buildCsvExport()`, etc., all filter by `_activeListId.value`
- ViewModel state: `_lists: mutableStateListOf<CounterList>`, `_activeListId: mutableStateOf<String>`, `_customOrderCache: HashMap<String, MutableList<String>>` (per-list order cache, not observable)
- `switchActiveList(listId)` saves current `_customOrder` to cache, swaps in the target list's order, calls `rebuildCustomOrder()`, and refreshes the value-sort snapshot
- List CRUD: `addList()`, `renameList(listId, name)`, `removeList(listId)` — removing a list deletes all its counters, groups, and order row from DB
- UI: `PrimaryScrollableTabRow` in `MainScreen`; tapping an **unselected** tab calls `switchActiveList()`; tapping the **already-selected** tab opens `ListSettingsDialog` (rename + export CSV + delete); a permanent "New List" tab at the end calls `addList()`
- `⋮` overflow menu in `MainScreen` provides: "Collapse All" / "Expand All" (writes `groupExpandedState`), "Import from CSV", "Delete All Groups" (`removeAllGroups()`), "Delete All Counters" (`removeAllCounters()`), "About Tracker"
- `listSequence` is incremented monotonically (same pattern as `counterSequence`/`groupSequence`)

### DisplayItem sealed class (`viewmodel/CounterViewModel.kt`)
The unified display list merges groups and ungrouped counters:
```kotlin
sealed class DisplayItem {
    data class Group(val group: CounterGroup) : DisplayItem()
    data class UngroupedCounter(val counter: Counter) : DisplayItem()
}
```
Only **ungrouped** counters appear in `DisplayItem` / `_customOrder`; grouped counters are accessed via `getCountersInGroup(groupId)`.

### Custom order invariant
Mutations usually update `_customOrder` directly and persist with `saveOrder()`. `rebuildCustomOrder()` is the reconciliation guard before custom-order reads/reorders (see `sortedDisplayItems()` and `reorderItems()`). `_customOrder` entries must always use the `"g:"` / `"c:"` prefix.

### Scroll-to-new-item signal
`_lastAddedKey` is set in `addCounter()`/`addGroup()` and cleared by `consumeLastAddedKey()` after the UI scrolls. Replicate this pattern when adding new top-level item types.

## State Management

- ViewModel state is `mutableStateListOf<>` / `mutableStateOf<>` — **not** StateFlow/LiveData
- `groupExpandedState: mutableStateMapOf<String, Boolean>` lives in `MainScreen` (UI-only, not persisted)
- Sort preference persisted via **DataStore** (`settings`); all other data via Room
- Value-based sorting uses `_valueSortSnapshot`; snapshot refresh is debounced by 3s in `scheduleValueSortSnapshot()` so VALUE sorts do not re-order on every +/- tap

## Auto-naming & Auto-color

- `counterSequence` / `groupSequence` are incremented monotonically on each add (not reset on delete)
- Colors are **not** auto-assigned on creation: `addGroup()` defaults `colorValue` to `0L`; `addCounter()` defaults `color` to `null`; both are set exclusively through their respective settings dialogs
- A single 10-color **pastel** palette (`colorOptions: List<Pair<String, Long>>` in `Dialogs.kt`) is shared by both `GroupSettingsDialog` and `CounterSettingsDialog`
- Both `GroupSettingsDialog` and `CounterSettingsDialog` support custom HSV colors via the inline `HsvColorPicker` composable; persist raw ARGB `Long` values (do not assume colors are limited to `colorOptions`)

## Build System

- Kotlin DSL Gradle with Version Catalog at `gradle/libs.versions.toml`
- KSP (not KAPT) for Room code generation — add new Room entities to `@Database(entities = […])` in `TrackerDatabase.kt`
- `TrackerDatabase` is currently at **schema version 3**; add a `Migration(n, n+1)` object and register it in `addMigrations(…)` for any schema change
- Compile/run via Android Studio or `./gradlew assembleDebug` / `./gradlew assembleRelease`
- Release signing reads env vars: `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`

## Key Dependencies (`gradle/libs.versions.toml`)

| Alias | Library | Purpose |
|---|---|---|
| `androidx.room.*` | Room 2.7.0 | Local DB (KSP-generated DAOs) |
| `androidx.datastore.preferences` | DataStore 1.1.7 | Sort-order preference |
| `androidx.navigation.compose` | Navigation 2.9.7 | `TrackerApp` NavHost |

**DAOs:** `CounterListDao`, `CounterDao`, `CounterGroupDao`, `CustomOrderDao` — all in `Daos.kt`.

## Custom Drag-to-Reorder (`ui/components/ReorderableList.kt`)

Drag-to-reorder is implemented in-project (no third-party library).

- `ReorderState` holds `draggingKey: String?` and `draggingOffset: Float` as drag state; current index is derived from the registered key → index map
- Drag ownership is tracked by stable key (`draggingKey`), not by index. Items register stable key → current index via `registerItem(key, index)` from a `SideEffect` on every recomposition.
- Each item applies `Modifier.pointerInput(stableKey, reorderState) { detectDragGesturesAfterLongPress(...) }` as its drag handle. Using `stableKey` (not index) ensures the gesture coroutine is NOT restarted when items swap positions.
- Swap threshold: when the dragged item's **centre** crosses the adjacent item's centre. After swap, offset is adjusted by the distance between the two items' `LazyListItemInfo.offset` values so the item stays visually under the finger.
- `rememberReorderState(lazyListState, onMove)` is the composable factory; `onMove` is refreshed each frame via `SideEffect`.

## Navigation

Two routes only, defined in `MainScreen.kt` inside `TrackerApp`:
- `"main"` → `MainScreen`
- `"about"` → `AboutScreen`

Add new top-level screens here and pass `navController` callbacks down.


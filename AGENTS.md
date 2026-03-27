# Tracker — AI Agent Guide

## Architecture Overview

Single-Activity MVVM Android app (Kotlin, Jetpack Compose, Material3).

```
MainActivity
  └─ TrackerTheme
       └─ TrackerApp (NavHost: "main" / "about")
            └─ MainScreen ─── CountersScreen
                                 ├─ GroupCard (+ CounterCard rows inside)
                                 └─ UngroupedCounterCard
```

**Layers:**
- `data/` — Room entities (`Counter`, `CounterGroup`, `CustomOrderEntity`), DAOs, `TrackerDatabase` singleton
- `viewmodel/CounterViewModel.kt` — single ViewModel; in-memory `mutableStateListOf` is the source of truth; DB is write-through persistence only (loaded once at `init`)
- `ui/components/` — all Composables; `ui/theme/` — Material3 theme

## Key Data Model Patterns

### Entity conventions
- IDs are `UUID.randomUUID().toString()` strings
- Group color stored as `Long` ARGB hex literal (e.g. `0xFF1E88E5L`); construct `Color(colorValue)` in UI
- `CustomOrderEntity` is a **single-row** table (`rowId = 0`); custom order serialized as comma-separated `"g:<uuid>"` / `"c:<uuid>"` tokens

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
After every mutation call `rebuildCustomOrder()` (it reconciles additions/deletions). `_customOrder` entries must always use the `"g:"` / `"c:"` prefix. See `reorderItems()` and `assignCounterToGroup()` for mutation patterns.

### Scroll-to-new-item signal
`_lastAddedKey` is set in `addCounter()`/`addGroup()` and cleared by `consumeLastAddedKey()` after the UI scrolls. Replicate this pattern when adding new top-level item types.

## State Management

- ViewModel state is `mutableStateListOf<>` / `mutableStateOf<>` — **not** StateFlow/LiveData
- `groupExpandedState: mutableStateMapOf<String, Boolean>` lives in `MainScreen` (UI-only, not persisted)
- Sort preference persisted via **DataStore** (`settings`); all other data via Room

## Auto-naming & Auto-color

- `counterSequence` / `groupSequence` are incremented monotonically on each add (not reset on delete)
- Group color auto-picks from the 10-color palette in `addGroup()` (avoids colors already in use, cycles by index as fallback); same palette is defined in `Dialogs.kt` as `groupColorOptions`

## Build System

- Kotlin DSL Gradle with Version Catalog at `gradle/libs.versions.toml`
- KSP (not KAPT) for Room code generation — add new Room entities to `@Database(entities = […])` in `TrackerDatabase.kt`
- Compile/run via Android Studio or `./gradlew assembleDebug` / `./gradlew assembleRelease`
- Release signing reads env vars: `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`

## Key Dependencies (`gradle/libs.versions.toml`)

| Alias | Library | Purpose |
|---|---|---|
| `androidx.room.*` | Room 2.7.0 | Local DB (KSP-generated DAOs) |
| `androidx.datastore.preferences` | DataStore 1.1.7 | Sort-order preference |
| `androidx.navigation.compose` | Navigation 2.9.0 | `TrackerApp` NavHost |

## Custom Drag-to-Reorder (`ui/components/ReorderableList.kt`)

Drag-to-reorder is implemented in-project (no third-party library).

- `ReorderState` holds `draggingIndex: Int?` and `draggingOffset: Float` (both `mutableStateOf`)
- Items register their stable key → current index via `registerItem(key, index)` called from a `SideEffect` on every recomposition. This decouples the stable `pointerInput` key from the ever-changing index.
- Each item applies `Modifier.pointerInput(stableKey, reorderState) { detectDragGesturesAfterLongPress(...) }` as its drag handle. Using `stableKey` (not index) ensures the gesture coroutine is NOT restarted when items swap positions.
- Swap threshold: when `|draggingOffset| >= adjacentItem.size / 2`. After swap, offset is adjusted by the distance between the two items' `LazyListItemInfo.offset` values so the item stays visually under the finger.
- `rememberReorderState(lazyListState, onMove)` is the composable factory; `onMove` is refreshed each frame via `SideEffect`.

## Navigation

Two routes only, defined in `MainScreen.kt` inside `TrackerApp`:
- `"main"` → `MainScreen`
- `"about"` → `AboutScreen`

Add new top-level screens here and pass `navController` callbacks down.


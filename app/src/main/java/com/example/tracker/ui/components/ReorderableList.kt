package com.example.tracker.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Holds all drag-to-reorder state for a LazyColumn.
 *
 * Gesture flow:
 *  1. Long press on an item calls [startDragByKey].
 *  2. Each pointer-move event calls [onDrag] with the Y delta.
 *  3. When the accumulated [draggingOffset] reaches half the adjacent item's
 *     height, [onMove] is fired and [draggingOffset] is adjusted so the item
 *     visually stays under the finger.
 *  4. Pointer up / cancel calls [endDrag].
 *
 * Items register their stable key → current index mapping via [registerItem]
 * (called from a SideEffect on every recomposition). This allows the
 * pointerInput coroutine to keep a stable key (avoiding gesture cancellation
 * on reorder) while still resolving the up-to-date index at drag-start time.
 */
class ReorderState(val lazyListState: LazyListState) {

    /** Updated each frame via SideEffect – never stale inside gesture handlers. */
    internal var onMove: (from: Int, to: Int) -> Unit = { _, _ -> }

    /** Maps each item's stable string key to its current index in the list. */
    private val keyToIndex = mutableMapOf<String, Int>()

    /** Index of the item currently being dragged, or null when idle. */
    var draggingIndex: Int? by mutableStateOf(null)
        private set

    /** Accumulated Y translation (px) applied to the dragging item. */
    var draggingOffset: Float by mutableStateOf(0f)
        private set

    // ── Called from SideEffect in every item's composition ───────────────────

    fun registerItem(key: String, index: Int) {
        keyToIndex[key] = index
    }

    // ── Called from gesture handlers ─────────────────────────────────────────

    /** Starts a drag for the item identified by [key]. */
    fun startDragByKey(key: String) {
        val index = keyToIndex[key] ?: return
        draggingIndex = index
        draggingOffset = 0f
    }

    /** Accumulates [delta] and swaps with an adjacent item if the threshold is crossed. */
    fun onDrag(delta: Float) {
        val index = draggingIndex ?: return
        draggingOffset += delta
        checkAndSwap(index)
    }

    fun endDrag() {
        draggingIndex = null
        draggingOffset = 0f
    }

    // ── Internal swap logic ───────────────────────────────────────────────────

    private fun checkAndSwap(index: Int) {
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val totalItems   = lazyListState.layoutInfo.totalItemsCount

        when {
            // Moving down: swap with next item when we've passed its midpoint
            draggingOffset > 0f && index + 1 < totalItems -> {
                val cur  = visibleItems.find { it.index == index }     ?: return
                val next = visibleItems.find { it.index == index + 1 } ?: return
                if (draggingOffset >= next.size / 2f) {
                    onMove(index, index + 1)
                    // Shift offset so the item stays visually under the finger
                    draggingOffset -= (next.offset - cur.offset).toFloat()
                    draggingIndex = index + 1
                }
            }
            // Moving up: swap with previous item when we've passed its midpoint
            draggingOffset < 0f && index > 0 -> {
                val cur  = visibleItems.find { it.index == index }     ?: return
                val prev = visibleItems.find { it.index == index - 1 } ?: return
                if (-draggingOffset >= prev.size / 2f) {
                    onMove(index, index - 1)
                    draggingOffset += (cur.offset - prev.offset).toFloat()
                    draggingIndex = index - 1
                }
            }
        }
    }
}

/**
 * Creates and remembers a [ReorderState] tied to [lazyListState].
 * [onMove] is updated every composition via SideEffect so it is never stale.
 */
@Composable
fun rememberReorderState(
    lazyListState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit
): ReorderState {
    val state = remember(lazyListState) { ReorderState(lazyListState) }
    SideEffect { state.onMove = onMove }
    return state
}


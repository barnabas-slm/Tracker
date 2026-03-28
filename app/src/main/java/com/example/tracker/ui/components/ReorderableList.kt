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
 *  3. When the dragged item's centre crosses the adjacent item's centre,
 *     [onMove] is fired and [draggingOffset] is adjusted so the item stays
 *     visually under the finger.
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

    /** Reverse lookup so swaps can update indices immediately before recomposition catches up. */
    private val indexToKey = mutableMapOf<Int, String>()

    /** Stable key of the item currently being dragged, or null when idle. */
    var draggingKey: String? by mutableStateOf(null)
        private set

    /** Index of the item currently being dragged, resolved from [draggingKey]. */
    val draggingIndex: Int?
        get() = draggingKey?.let { keyToIndex[it] }

    /** Accumulated Y translation (px) applied to the dragging item. */
    var draggingOffset: Float by mutableStateOf(0f)
        private set

    // ── Called from SideEffect in every item's composition ───────────────────

    fun registerItem(key: String, index: Int) {
        keyToIndex[key] = index
        indexToKey[index] = key
    }

    fun isDragging(key: String): Boolean = draggingKey == key

    // ── Called from gesture handlers ─────────────────────────────────────────

    /** Starts a drag for the item identified by [key]. */
    fun startDragByKey(key: String) {
        if (keyToIndex[key] == null) return
        draggingKey = key
        draggingOffset = 0f
    }

    /** Accumulates [delta] and swaps with an adjacent item if the threshold is crossed. */
    fun onDrag(delta: Float) {
        if (draggingIndex == null) return
        draggingOffset += delta
        // Only perform one swap per pointer event.
        // Running multiple swaps against the same (stale) layout snapshot can
        // over-correct offset and make the dragged item jump away from the finger.
        checkAndSwap()
    }

    fun endDrag() {
        draggingKey = null
        draggingOffset = 0f
    }

    // ── Internal swap logic ───────────────────────────────────────────────────

    private fun checkAndSwap(): Boolean {
        val index = draggingIndex ?: return false
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val totalItems   = lazyListState.layoutInfo.totalItemsCount
        val cur = visibleItems.find { it.index == index } ?: return false
        val draggedCenter = cur.offset + draggingOffset + (cur.size / 2f)

        when {
            // Moving down: swap once the dragged item's centre passes the next item's centre.
            draggingOffset > 0f && index + 1 < totalItems -> {
                val next = visibleItems.find { it.index == index + 1 } ?: return false
                val nextCenter = next.offset + (next.size / 2f)
                if (draggedCenter > nextCenter) {
                    onMove(index, index + 1)
                    swapTrackedIndices(index, index + 1)
                    // Moving down across different heights needs an adjusted
                    // distance: base offset increases by next.size + spacing.
                    val downwardSwapDistance =
                        (next.offset - cur.offset) + (next.size - cur.size)
                    draggingOffset -= downwardSwapDistance.toFloat()
                    return true
                }
            }
            // Moving up: swap once the dragged item's centre passes the previous item's centre.
            draggingOffset < 0f && index > 0 -> {
                val prev = visibleItems.find { it.index == index - 1 } ?: return false
                val prevCenter = prev.offset + (prev.size / 2f)
                if (draggedCenter < prevCenter) {
                    onMove(index, index - 1)
                    swapTrackedIndices(index, index - 1)
                    draggingOffset += (cur.offset - prev.offset).toFloat()
                    return true
                }
            }
        }

        return false
    }

    private fun swapTrackedIndices(from: Int, to: Int) {
        val fromKey = indexToKey[from]
        val toKey = indexToKey[to]

        if (fromKey != null) {
            keyToIndex[fromKey] = to
            indexToKey[to] = fromKey
        } else {
            indexToKey.remove(to)
        }

        if (toKey != null) {
            keyToIndex[toKey] = from
            indexToKey[from] = toKey
        } else {
            indexToKey.remove(from)
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


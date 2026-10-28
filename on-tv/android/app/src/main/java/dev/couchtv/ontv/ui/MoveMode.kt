package dev.couchtv.ontv.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * The remote's "move an app" gesture: once an app is held (long-press OK), the D-pad slides it
 * instead of moving focus. Left/Right step one place; Up/Down step a whole row in a grid ([cols]),
 * or are swallowed in a single row so the held app can't wander off. Back cancels (the caller puts
 * the old order back). OK is left to the tile's own click, which places it. Both key-down and
 * key-up are consumed so nothing leaks to the screen's own back or focus handling while held.
 */
/**
 * Makes a row's ends solid: Left on the first item and Right on the last are swallowed, so focus
 * stays in the row instead of jumping to whatever happens to lie that way on screen (the avatar,
 * a nav tab). Apply to each item with its position.
 */
fun Modifier.rowEnds(first: Boolean, last: Boolean): Modifier =
    if (!first && !last) this else onPreviewKeyEvent { e ->
        (first && e.key == Key.DirectionLeft) || (last && e.key == Key.DirectionRight)
    }

fun Modifier.moveKeys(active: Boolean, onMove: (Int) -> Unit, onCancel: () -> Unit, cols: Int = 0): Modifier =
    if (!active) this else onPreviewKeyEvent { e ->
        val down = e.type == KeyEventType.KeyDown
        when (e.key) {
            Key.DirectionLeft -> { if (down) onMove(-1); true }
            Key.DirectionRight -> { if (down) onMove(1); true }
            Key.DirectionUp -> { if (down && cols > 0) onMove(-cols); true }
            Key.DirectionDown -> { if (down && cols > 0) onMove(cols); true }
            Key.Back, Key.Escape -> { if (!down) onCancel(); true }
            else -> false
        }
    }

package dev.couchtv.ontv.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj

/** The gap between keys; key size comes from the width the keyboard is given. */
private val KeyGap = 6.dp

/**
 * The remote's keyboard for search: A–Z and 0–9 in six-key rows, then backspace, Space, Clear and
 * Search. Square keys sized from the column's width, so it fills its share of the screen exactly.
 * D-pad between keys, OK to type. Leaving to the results and coming back lands on the key that was
 * last used ([focusRestorer]); [keyboardFocus] asks for the keyboard as a whole.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchKeyboard(
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    keyboardFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    val rows = listOf("ABCDEF", "GHIJKL", "MNOPQR", "STUVWX", "YZ1234", "567890")
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val total = maxWidth
        val key = (total - KeyGap * 5) / 6
        // requester → restorer → group: asking for the keyboard (Back from results) re-enters the
        // group, and the restorer sends focus to the last key used instead of the first one
        Column(Modifier.focusRequester(keyboardFocus).focusRestorer().focusGroup(), verticalArrangement = Arrangement.spacedBy(KeyGap)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                    row.forEach { ch -> Key(key, key, onOk = { onChar(ch.lowercaseChar()) }) { KeyText(ch.toString()) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                val wide = (total - key - KeyGap * 3) / 3
                Key(key, key, onOk = onBackspace) { KeyIcon(SearchIcons.Backspace, "Delete") }
                Key(wide, key, onOk = onSpace) { KeyText("Space") }
                Key(wide, key, onOk = onClear) { KeyText("Clear") }
                Key(wide, key, onOk = onSearch) { KeyText("Search") }
            }
        }
    }
}

@Composable
private fun Key(width: Dp, height: Dp, onOk: () -> Unit, content: @Composable () -> Unit) {
    val shape = Cj.CardSmall
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x1FFFFFFF), contentColor = Color.White.copy(alpha = 0.82f),
            focusedContainerColor = Color(0x38FFFFFF), focusedContentColor = Color.White,
            pressedContainerColor = Color(0x52FFFFFF)),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), shape = shape),
            focusedBorder = Border(BorderStroke(1.5.dp, Color.White), shape = shape)),
        modifier = Modifier.width(width).height(height)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun KeyText(t: String) =
    Text(t, style = Cj.Label.copy(color = Color.Unspecified, fontSize = 11.sp))

@Composable
private fun KeyIcon(icon: ImageVector, label: String) =
    Icon(icon, contentDescription = label, modifier = Modifier.size(13.dp))

package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj

/**
 * An on-screen alphabet grid for the remote: d-pad across the keys, OK to type. A wide space and
 * delete sit on top, then a–z and 0–9. White-on-focus, matching the header's monochrome look.
 */
@Composable
fun TvKeyboard(
    onChar: (Char) -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    firstKey: FocusRequester? = null
) {
    val rows = listOf("abcdef", "ghijkl", "mnopqr", "stuvwx", "yz1234", "567890")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            KeyCap(onOk = onSpace, modifier = Modifier.weight(1f)) {
                // a mid-grey space bar reads on both the dark idle key and the white focused one
                Box(Modifier.width(30.dp).height(3.dp).background(Cj.Dim, CircleShape))
            }
            KeyCap(onOk = onDelete, modifier = Modifier.weight(1f)) {
                Text("⌫", style = Cj.Label.copy(color = Color.Unspecified))
            }
        }
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { ch ->
                    val isFirst = ch == 'a' && firstKey != null
                    KeyCap(
                        onOk = { onChar(ch) },
                        modifier = Modifier.weight(1f)
                            .then(if (isFirst) Modifier.focusRequester(firstKey!!) else Modifier)
                    ) {
                        Text(ch.toString(), style = Cj.Label.copy(color = Color.Unspecified))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyCap(onOk: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Cj.Surface2, contentColor = Cj.Text,
            focusedContainerColor = Color.White, focusedContentColor = Cj.Ink),
        modifier = modifier
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

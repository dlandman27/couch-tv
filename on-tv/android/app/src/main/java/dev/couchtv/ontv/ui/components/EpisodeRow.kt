package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo

/** One episode in the series overlay: its number and title, white when focused. */
@Composable
fun EpisodeRow(e: UiRepo.Episode, onOk: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent, contentColor = Cj.Text,
            focusedContainerColor = Color.White, focusedContentColor = Cj.Ink),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text("E${e.num}", style = Cj.Label.copy(color = Color.Unspecified), modifier = Modifier.width(52.dp))
            Text(e.title.ifEmpty { "Episode ${e.num}" }, style = Cj.Label.copy(color = Color.Unspecified),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

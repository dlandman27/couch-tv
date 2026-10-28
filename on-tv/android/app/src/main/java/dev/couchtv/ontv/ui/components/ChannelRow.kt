package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj

/** One live channel: its logo and name in a row, white when it holds focus. */
@Composable
fun ChannelRow(name: String, logo: String, onOk: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Cj.Surface.copy(alpha = 0.55f),
            focusedContainerColor = androidx.compose.ui.graphics.Color.White,
            focusedContentColor = Cj.Ink),
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
            AsyncImage(model = logo.ifEmpty { null }, contentDescription = null,
                contentScale = ContentScale.Fit, modifier = Modifier.size(34.dp))
            Text(name, style = Cj.Label.copy(color = androidx.compose.ui.graphics.Color.Unspecified),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 14.dp))
        }
    }
}

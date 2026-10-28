package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj

/** A Your Teams or Coming Up row: logo, a name line, a state line that glows while live. */
@Composable
fun TeamRow(name: String, sub: String, logo: String, live: Boolean, onOk: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent, contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.12f), focusedContentColor = Color.White),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            AsyncImage(model = logo.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(34.dp))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(name, style = Cj.Label.copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, style = Cj.Small.copy(color = if (live) Color(0xFFFF6B6B) else Cj.Dim), maxLines = 1)
            }
            Text("›", style = Cj.Label.copy(color = Cj.Dim))
        }
    }
}

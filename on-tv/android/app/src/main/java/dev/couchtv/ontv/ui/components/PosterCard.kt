package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.Stars

/** A 2:3 poster with its title and star row under it: the film and series card everywhere. */
@Composable
fun PosterCard(
    name: String,
    poster: String,
    rating: Double,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp
) {
    Column(modifier = modifier.width(width)) {
        Surface(
            onClick = onOk,
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.07f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Cj.Surface, focusedContainerColor = Cj.Surface),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(12.dp))),
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
        ) {
            // a dim card shows under the poster while it decodes, so a rail never flashes empty
            Box(
                Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp))
                    .background(Cj.Surface2)) {
                AsyncImage(
                    model = poster.ifEmpty { null }, contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
            }
        }
        Text(name, style = Cj.Small.copy(color = Cj.Text), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp, start = 2.dp))
        Stars(rating, size = Cj.Small.fontSize)
    }
}

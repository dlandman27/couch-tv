package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj

/** A 16:9 banner card with a name over its bottom fade: apps, keep-watching, anything wide. */
@Composable
fun WideCard(
    name: String,
    image: Any?,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 230.dp,
    showName: Boolean = true
) {
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors = ClickableSurfaceDefaults.colors(containerColor = Cj.Surface, focusedContainerColor = Cj.Surface),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(12.dp))),
        modifier = modifier.width(width).aspectRatio(16f / 9f)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(model = image, contentDescription = name, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
            if (showName) {
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xD904060C))))
                Text(name, style = Cj.Label.copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
            }
        }
    }
}

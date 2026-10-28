package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj

/**
 * A Continue Watching tile, reference-style: a 16:9 backdrop with a thin resume bar, and beneath
 * the card the title and, when known, the remaining time. On D-pad
 * focus it lifts slightly and takes a clean white outline. When the title has no artwork it falls
 * back to a tasteful dark panel carrying the title, never a random stock image. OK resumes playback.
 */
@Composable
fun ContinueCard(
    name: String,
    image: String,
    remaining: String?,
    progress: Float?,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Cj.RailCard
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.width(width)) {
        Surface(
            onClick = onOk,
            shape = ClickableSurfaceDefaults.shape(Cj.Card),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Cj.Surface, focusedContainerColor = Cj.Surface),
            border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = Cj.Card)),
            modifier = Modifier.onFocusChanged { focused = it.isFocused }.fillMaxWidth().aspectRatio(16f / 9f)
        ) {
            Box(Modifier.fillMaxSize()) {
                if (image.isNotEmpty()) {
                    AsyncImage(model = image, contentDescription = name, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                } else {
                    // dark fallback — a quiet gradient panel with the title, never a stock photo
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        listOf(Color(0xFF1C1E24), Color(0xFF101116)))), contentAlignment = Alignment.Center) {
                        Text(name, style = Cj.Small.copy(color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp),
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
                // a subtle resume bar, always present, flush to the bottom edge
                if (progress != null) Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).background(Color(0x4DFFFFFF))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0.03f, 1f)).background(Cj.Accent))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(name, style = Cj.Label.copy(color = Color.White.copy(alpha = if (focused) 1f else 0.82f), fontSize = 10.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        // remaining time whenever it's known; the line is always reserved so cards stay aligned
        Box(Modifier.height(11.dp)) {
            if (remaining != null)
                Text(remaining, style = Cj.Small.copy(color = Color.White.copy(alpha = if (focused) 0.75f else 0.6f), fontSize = 8.5.sp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

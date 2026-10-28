package dev.couchtv.ontv.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.AppTile
import dev.couchtv.ontv.ui.Cj

/**
 * One app as a uniform rounded-square tile with its name beneath, Apple-TV style. Every app gets the
 * same tile: adaptive icons fill it edge to edge, legacy icons sit centred on a quiet dark tile, and
 * a [glyph] (e.g. All Apps) draws a symbol on that same dark tile. Used in the Apps grid and Home's
 * app row. Optional edit affordances (pin/hide badge, held, dimmed).
 */
@Composable
fun AppIcon(
    label: String,
    icon: Any?,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 104.dp,
    glyph: ImageVector? = null,
    badge: String? = null,
    dim: Boolean = false,
    held: Boolean = false
) {
    val tile = remember(icon) { (icon as? Drawable)?.let { runCatching { AppTile.render(it) }.getOrNull() } }
    var focused by remember { mutableStateOf(false) }
    // the column is wider than the tile so labels like "YouTube Music" fit on one line
    Column(modifier = modifier.width(size + 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      // the focus ring sits just outside the tile with a small gap, Apple-TV style, so it reads
      // on every icon — including white ones, where an outline on the tile's edge disappears
      // a held app lifts as one unit (tile and ring together), so the gap between them never closes
      Box(Modifier.graphicsLayer { val s = if (held) 1.08f else 1f; scaleX = s; scaleY = s }
          .size(size + 6.dp).then(
          if (focused) Modifier.border(if (held) 2.dp else 1.5.dp, Color.White, RoundedCornerShape(9.dp)) else Modifier),
          contentAlignment = Alignment.Center) {
        Surface(
            onClick = onOk,
            onLongClick = onLongClick,
            shape = ClickableSurfaceDefaults.shape(Cj.Card),
            scale = ClickableSurfaceDefaults.scale(focusedScale = if (held) 1f else 1.04f),
            // a held app sits slightly above the row: a soft dark drop shadow, not a glow
            glow = ClickableSurfaceDefaults.glow(focusedGlow =
                if (held) androidx.tv.material3.Glow(elevationColor = Color(0xCC000000), elevation = 14.dp)
                else androidx.tv.material3.Glow.None),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = Cj.Card),
                focusedBorder = Border.None),
            modifier = Modifier.onFocusChanged { focused = it.isFocused }.size(size)
        ) {
            Box(Modifier.fillMaxSize()) {
                // the quiet dark tile every non-full-bleed icon (and the glyph) sits on
                if (tile?.fullBleed != true) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color(0xFF26282F), Color(0xFF17181D)))))
                when {
                    glyph != null -> Icon(glyph, contentDescription = label, tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.Center).size(size * 0.42f))
                    tile != null -> Image(tile.image, contentDescription = label, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                    else -> AsyncImage(model = icon, contentDescription = label, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                }
                if (dim) Box(Modifier.fillMaxSize().background(Color(0xB3050506)))
                if (badge != null) Box(Modifier.align(Alignment.TopEnd).padding(5.dp)
                    .size(24.dp).background(Color(0xCC0A0A0B), RoundedCornerShape(999.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(badge, style = Cj.Small.copy(color = Color.White))
                }
            }
        }
      }
        // the ring's 3dp margin already separates tile and label, so the gap stays as before
        Text(label, style = Cj.Small.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 9.sp,
            shadow = androidx.compose.ui.graphics.Shadow(Color(0xCC000000), androidx.compose.ui.geometry.Offset(0f, 1f), 5f)),
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp))
    }
}

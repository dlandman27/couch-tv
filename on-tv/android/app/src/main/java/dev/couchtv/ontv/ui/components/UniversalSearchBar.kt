package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.NavIcons

/**
 * Orbit's universal search: a restrained, Spotlight-like field. A quiet dark surface over the
 * wallpaper, a search glyph and prompt, a mic on the right. One focus treatment — a clean 1.5px
 * white hairline — nothing glowing or oversized. Opening it hands off to the full search screen.
 */
@Composable
fun UniversalSearchBar(onOk: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)   // fully round ends (pill), as in the reference
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),   // no zoom — restraint
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x99101217),                       // quiet translucent charcoal
            contentColor = Color(0xFF9A9AA2),
            focusedContainerColor = Color(0xE60B0D11),                // a touch more solid when focused
            focusedContentColor = Color(0xFFC8C8CE)),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)),
        modifier = modifier.width(416.dp).height(40.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(start = 20.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(NavIcons.Search, contentDescription = "Search", modifier = Modifier.size(13.dp))
            Text("Search or ask Orbit", style = Cj.Body.copy(color = Color.Unspecified, fontSize = 12.sp),
                modifier = Modifier.weight(1f).padding(start = 11.dp))
            Icon(NavIcons.Mic, contentDescription = "Voice search", modifier = Modifier.size(13.dp))
        }
    }
}

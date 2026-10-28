package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj

/**
 * A compact, understated top nav: quiet text+icon tabs, the current screen marked only by a thin
 * underline, the one D-pad-focused tab by a single white hairline. No oversized pill, no filled
 * selected state — selected and focused never look the same.
 */
@Composable
fun NavBar(
    icons: List<ImageVector>,
    names: List<String>,
    selected: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabFocus: FocusRequester? = null,
    downTarget: FocusRequester? = null
) {
    Row(modifier.then(if (downTarget != null) Modifier.focusProperties { down = downTarget } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        icons.forEachIndexed { i, icon ->
            NavTab(icon, names.getOrElse(i) { "" }, on = selected == i, onClick = { onPick(i) },
                modifier = if (i == selected && tabFocus != null) Modifier.focusRequester(tabFocus) else Modifier)
        }
    }
}

@Composable
private fun NavTab(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(7.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            // selected reads bright; the rest are a quiet but legible grey — distinct from focus
            contentColor = if (on) Color.White else Color.White.copy(alpha = 0.72f),
            focusedContainerColor = Color(0x26FFFFFF),
            focusedContentColor = Color.White),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.5.dp, Color.White), shape = shape)),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(13.dp))
                Text(label, style = Cj.Label.copy(color = Color.Unspecified, fontSize = 13.sp))
            }
            // the current screen gets a short bright underline; focus is shown by the box + border
            Box(Modifier.padding(top = 3.dp).height(2.dp).width(if (on) 16.dp else 0.dp)
                .background(Color.White, CircleShape))
        }
    }
}

/** A circular nav target: quiet, a single white hairline on focus (search, avatar). */
@Composable
fun NavIconButton(
    icon: ImageVector,
    label: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    idleContainer: Color = Color.Transparent
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = idleContainer,
            contentColor = Color.White.copy(alpha = if (on) 0.95f else 0.62f),
            focusedContainerColor = Color(0x1FFFFFFF),
            focusedContentColor = Color.White),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.5.dp, Color.White), shape = CircleShape)),
        modifier = modifier
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.padding(7.dp).size(13.dp))
    }
}

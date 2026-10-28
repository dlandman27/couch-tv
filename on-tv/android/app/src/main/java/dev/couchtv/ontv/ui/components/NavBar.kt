package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj

/**
 * The glass nav: a pill of icon-only tabs, then the Search icon and the avatar on the right.
 * The selected tab's icon fills; the rest are dim. Search is a tab too (its index), just placed
 * apart so it sits with the account controls, the way the reference design groups them.
 */
@Composable
fun NavBar(
    icons: List<ImageVector>,
    names: List<String>,
    selected: Int,
    searchIcon: ImageVector,
    searchSelected: Boolean,
    who: String,
    onPick: (Int) -> Unit,
    onSearch: () -> Unit,
    onWho: () -> Unit,
    modifier: Modifier = Modifier,
    tabFocus: FocusRequester? = null
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // the tab pill (near-solid, so it reads clearly over a bright hero)
        val pillColor = Color(0xF21A1424)
        Surface(
            onClick = {}, enabled = false,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(containerColor = pillColor, disabledContainerColor = pillColor)
        ) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                icons.forEachIndexed { i, icon ->
                    NavIcon(icon, names.getOrElse(i) { "" }, on = selected == i,
                        onClick = { onPick(i) },
                        modifier = if (i == selected && tabFocus != null) Modifier.focusRequester(tabFocus) else Modifier)
                }
            }
        }
        // divider, then Search + avatar on the right
        Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            NavIcon(searchIcon, "Search", on = searchSelected, onClick = onSearch, boxed = true)
            Surface(
                onClick = onWho,
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = CircleShape)),
                modifier = Modifier.padding(start = 10.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(38.dp).background(
                        Brush.linearGradient(listOf(Color(0xFFC97B4A), Color(0xFF7A4AC9))), CircleShape)
                ) { Text(who.take(1).uppercase(), style = Cj.Label.copy(color = Color.White)) }
            }
        }
    }
}

/** One nav icon: a circular target, white-filled when selected, white ring when focused. */
@Composable
private fun NavIcon(
    icon: ImageVector,
    label: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    boxed: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (on) Color.White else Color.Transparent,
            contentColor = if (on) Cj.Ink else Color.White.copy(alpha = 0.6f),
            focusedContainerColor = Color.White, focusedContentColor = Cj.Ink),
        modifier = modifier
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.padding(10.dp).size(22.dp))
    }
}

package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import androidx.compose.foundation.BorderStroke
import dev.couchtv.ontv.ui.Cj

/**
 * The glass nav: tab pills with the reference design's sliding white glow, then the avatar.
 * The pill indicator and its slide come with the TV TabRow.
 */
@Composable
fun NavBar(
    tabs: List<String>,
    selected: Int,
    who: String,
    onPick: (Int) -> Unit,
    onWho: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = {},
            enabled = false,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(containerColor = Cj.Surface.copy(alpha = 0.7f), disabledContainerColor = Cj.Surface.copy(alpha = 0.7f))
        ) {
            TabRow(
                selectedTabIndex = selected,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                indicator = { tabPositions, doesTabRowHaveFocus ->
                    tabPositions.getOrNull(selected)?.let {
                        TabRowDefaults.PillIndicator(
                            currentTabPosition = it,
                            doesTabRowHaveFocus = doesTabRowHaveFocus,
                            activeColor = Color.White,
                            inactiveColor = Color.White.copy(alpha = 0.9f))
                    }
                }
            ) {
                tabs.forEachIndexed { i, name ->
                    Tab(
                        selected = selected == i,
                        onFocus = { onPick(i) },
                        colors = TabDefaults.pillIndicatorTabColors(
                            contentColor = Color.White.copy(alpha = 0.6f),
                            focusedContentColor = Cj.Ink,
                            selectedContentColor = Cj.Ink)
                    ) {
                        Text(name, style = Cj.Label.copy(color = Color.Unspecified),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                    }
                }
            }
        }
        Surface(
            onClick = onWho,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
            border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = CircleShape)),
            modifier = Modifier.padding(start = 16.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp).background(
                    Brush.linearGradient(listOf(Color(0xFFC97B4A), Color(0xFF7A4AC9))), CircleShape)
            ) { Text(who.take(1).uppercase(), style = Cj.Label.copy(color = Color.White)) }
        }
    }
}

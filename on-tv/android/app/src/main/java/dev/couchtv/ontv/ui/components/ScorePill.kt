package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo
import kotlinx.coroutines.delay

/** The floating score pill: the followed games taking turns, OK jumps to Sports. */
@Composable
fun ScorePill(games: List<UiRepo.Game>, follow: List<String>, onOk: () -> Unit, modifier: Modifier = Modifier) {
    if (games.isEmpty()) return
    var at by remember { mutableIntStateOf(0) }
    LaunchedEffect(games.size) { while (true) { delay(6000); if (games.size > 1) at = (at + 1) % games.size } }
    val g = games[at % games.size]

    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xEB221C30), contentColor = Color.White,
            focusedContainerColor = Color(0xFF2B2440), focusedContentColor = Color.White),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = CircleShape)),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            @Composable fun side(s: UiRepo.Side) {
                AsyncImage(model = s.logo.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(22.dp))
                Text(s.abbr, style = Cj.Label.copy(color = if (s.abbr in follow) Color(0xFF8FD3FF) else Color.White))
                if (g.state != "pre") Text(s.score, style = Cj.Label.copy(color = Color.White))
            }
            side(g.away)
            Text("—", style = Cj.Label.copy(color = Cj.Dim))
            side(g.home)
            Text(g.detail, style = Cj.Small.copy(color = Cj.Dim), modifier = Modifier.padding(start = 6.dp))
            Text("›", style = Cj.Label.copy(color = Cj.Dim))
        }
    }
}

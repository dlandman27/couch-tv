package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

/** One game in the sports strip: league and clock up top, the two sides with logos under. */
@Composable
fun GameCard(g: UiRepo.Game, leagueName: String, follow: List<String>, hideScores: Boolean,
             onOk: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Cj.Surface.copy(alpha = 0.75f), contentColor = Color.White,
            focusedContainerColor = Cj.Hover, focusedContentColor = Color.White),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(14.dp))),
        modifier = modifier.width(230.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(leagueName, style = Cj.Small)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (g.state == "in") Box(Modifier.size(7.dp).background(Color(0xFFFF4D4D), CircleShape).padding(end = 4.dp))
                    Text((if (g.state == "in") " " else "") + g.detail, style = Cj.Small.copy(
                        color = if (g.state == "in") Color(0xFFFFD166) else Cj.Dim))
                }
            }
            @Composable fun side(s: UiRepo.Side) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    AsyncImage(model = s.logo.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(26.dp))
                    Text(s.abbr, style = Cj.Label.copy(color = if (s.abbr in follow) Color(0xFF8FD3FF) else Color.White),
                        modifier = Modifier.padding(start = 10.dp).weight(1f))
                    if (g.state != "pre" && !hideScores) Text(s.score, style = Cj.Label.copy(color = Color.White))
                }
            }
            side(g.away)
            side(g.home)
        }
    }
}

package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo

/** A series over whatever page opened it: season chips along the top, the episodes below. */
@Composable
fun EpisodesOverlay(name: String, seasons: List<UiRepo.Season>, onPlay: (UiRepo.Episode) -> Unit) {
    var season by remember { mutableIntStateOf(0) }
    val firstEp = remember { androidx.compose.ui.focus.FocusRequester() }
    // the remote lands on the first episode the moment the list is in
    androidx.compose.runtime.LaunchedEffect(seasons.isNotEmpty()) {
        if (seasons.isNotEmpty()) runCatching { firstEp.requestFocus() }
    }
    Box(Modifier.fillMaxSize().background(Color(0xF2050505)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(680.dp).heightIn(max = 560.dp)
                .background(Cj.Surface.copy(alpha = 0.97f), RoundedCornerShape(20.dp)).padding(28.dp)
        ) {
            Text(name, style = Cj.RowTitle, modifier = Modifier.padding(bottom = 14.dp))
            if (seasons.isEmpty()) {
                Text("Nothing came back for this series.", style = Cj.Body)
                return@Column
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                seasons.forEachIndexed { i, se ->
                    FilterChip("S${se.season}", on = i == season, onOk = { season = i })
                }
            }
            LazyColumn {
                val eps = seasons[season.coerceIn(0, seasons.size - 1)].episodes
                items(eps, key = { it.id }) { e ->
                    EpisodeRow(e, onOk = { onPlay(e) },
                        modifier = if (e === eps.firstOrNull())
                            androidx.compose.ui.Modifier.focusRequester(firstEp) else androidx.compose.ui.Modifier)
                }
            }
        }
    }
}

package dev.couchtv.ontv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo
import dev.couchtv.ontv.ui.components.FilterChip
import dev.couchtv.ontv.ui.components.GameCard
import dev.couchtv.ontv.ui.components.TeamRow

/** Sports: filter chips, the game strip, a featured game hero, and the Your Teams panel. */
@Composable
fun SportsScreen(cfg: UiRepo.SportsConfig, games: List<UiRepo.Game>) {
    val sportOf = mapOf("nfl" to "Football", "ncaaf" to "Football", "nba" to "Basketball",
        "mlb" to "Baseball", "nhl" to "Hockey", "mls" to "Soccer", "eng.1" to "Soccer", "uefa.champions" to "Soccer")
    var filter by rememberSaveable { mutableStateOf("all") }
    var hide by rememberSaveable { mutableStateOf(false) }
    var featId by remember { mutableStateOf<String?>(null) }

    fun followed(g: UiRepo.Game) = g.home.abbr in cfg.follow || g.away.abbr in cfg.follow
    val rank = mapOf("in" to 0, "pre" to 1, "post" to 2)
    val list = games.filter { filter == "all" || sportOf[it.league] == filter }
        .sortedWith(compareBy({ rank[it.state] ?: 3 }, { !followed(it) }, { it.start }))
    val feat = list.firstOrNull { it.id == featId }
        ?: list.maxByOrNull { (if (it.state == "in") 4 else if (it.state == "pre") 2 else 0) + (if (followed(it)) 3 else 0) }

    val chips = listOf("all" to "All Sports") +
        cfg.leagues.mapNotNull { sportOf[it] }.distinct().map { it to it } + listOf("hide" to "Hide Scores")

    Column(Modifier.fillMaxSize().padding(horizontal = 44.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp)) {
            Text("Sports", style = Cj.RowTitle, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { (id, name) ->
                    FilterChip(name, on = if (id == "hide") hide else filter == id, onOk = {
                        if (id == "hide") hide = !hide else { filter = id; featId = null }
                    })
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(list, key = { _, g -> g.id }) { _, g ->
                GameCard(g, cfg.leagueNames[g.league] ?: g.league, cfg.follow, hide, onOk = { featId = g.id })
            }
        }
        if (list.isEmpty()) Text("No games today.", style = Cj.Body, modifier = Modifier.padding(top = 30.dp))
        Row(Modifier.padding(top = 18.dp).weight(1f)) {
            Box(Modifier.weight(1f).fillMaxSize()
                .background(Brush.linearGradient(listOf(Cj.Surface2, Cj.Surface.copy(alpha = 0.4f))), RoundedCornerShape(18.dp))) {
                feat?.let { g ->
                    Column(Modifier.align(Alignment.BottomStart).padding(28.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            AsyncImage(model = g.away.logo.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(84.dp))
                            Text("at", style = Cj.Body.copy(color = Cj.Dim))
                            AsyncImage(model = g.home.logo.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(84.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                            if (g.state == "in") Text("● LIVE   ", style = Cj.Small.copy(color = Color(0xFFFF4D4D)))
                            Text(cfg.leagueNames[g.league] ?: g.league, style = Cj.Small)
                        }
                        Text("${g.away.name} at ${g.home.name}", style = Cj.RowTitle)
                        Text(g.detail + (if (g.state != "pre" && !hide) "  ·  ${g.away.score}–${g.home.score}" else ""),
                            style = Cj.Body.copy(color = Cj.Text))
                    }
                }
            }
            Column(Modifier.width(330.dp).padding(start = 22.dp)) {
                Text("Your Teams", style = Cj.Label.copy(color = Cj.Dim), modifier = Modifier.padding(bottom = 6.dp))
                LazyColumn {
                    itemsIndexed(cfg.follow, key = { _, t -> t }) { _, abbr ->
                        val g = games.firstOrNull { it.away.abbr == abbr || it.home.abbr == abbr }
                        val side = if (g?.away?.abbr == abbr) g.away else g?.home
                        val sub = when {
                            g == null -> "No game today"
                            g.state == "in" -> "Playing now" + if (hide) "" else " · ${g.away.score}–${g.home.score}"
                            g.state == "pre" -> g.detail
                            else -> "Final" + if (hide) "" else " · ${g.away.score}–${g.home.score}"
                        }
                        TeamRow(side?.name ?: abbr, sub, side?.logo ?: "", live = g?.state == "in",
                            onOk = { g?.let { featId = it.id } })
                    }
                }
            }
        }
    }
}

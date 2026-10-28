package dev.couchtv.ontv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo
import dev.couchtv.ontv.ui.components.FilterChip
import dev.couchtv.ontv.ui.components.GameCard
import dev.couchtv.ontv.ui.components.SkeletonBox

/**
 * Sports, the scoreboard way: a Live Now board of games in progress, then Upcoming Games, each a
 * focusable card that opens the game. A league filter and a scores toggle sit up top. Followed
 * teams float to the front of each row.
 */
@Composable
fun SportsScreen(cfg: UiRepo.SportsConfig, games: List<UiRepo.Game>) {
    val sportOf = mapOf("nfl" to "Football", "ncaaf" to "Football", "nba" to "Basketball",
        "mlb" to "Baseball", "nhl" to "Hockey", "mls" to "Soccer", "eng.1" to "Soccer", "uefa.champions" to "Soccer")
    var filterIdx by rememberSaveable { mutableStateOf(0) }
    var hide by rememberSaveable { mutableStateOf(false) }
    // tell "still loading the scoreboard" apart from "there really are no games"
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); settled = true }
    LaunchedEffect(games) { if (games.isNotEmpty()) settled = true }

    fun followed(g: UiRepo.Game) = g.home.abbr in cfg.follow || g.away.abbr in cfg.follow

    val leagues = listOf("all" to "All Leagues") +
        cfg.leagues.mapNotNull { sportOf[it] }.distinct().map { it to it }
    val activeSport = leagues[filterIdx.coerceIn(0, leagues.size - 1)].first

    val shown = games.filter { activeSport == "all" || sportOf[it.league] == activeSport }
    val live = shown.filter { it.state == "in" }.sortedWith(compareBy({ !followed(it) }, { it.start }))
    val upcoming = shown.filter { it.state == "pre" }.sortedWith(compareBy({ !followed(it) }, { it.start }))
    val finals = shown.filter { it.state == "post" }.sortedWith(compareBy({ !followed(it) }, { it.start }))

    Column(Modifier.fillMaxSize().padding(start = 44.dp, end = 44.dp, top = 54.dp)) {
        // Live Now header with the league filter and the scores toggle on the right
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Text("Live Now", style = Cj.RowTitle, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(leagues[filterIdx.coerceIn(0, leagues.size - 1)].second + "  ▾", on = false,
                    onOk = { filterIdx = (filterIdx + 1) % leagues.size })
                FilterChip(if (hide) "Scores Off" else "Scores On", on = !hide, onOk = { hide = !hide })
            }
        }

        if (games.isEmpty() && !settled) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed((0..5).toList()) { _, _ -> SkeletonBox(Modifier.width(230.dp).height(132.dp), corner = 14.dp) }
            }
            return@Column
        }

        if (live.isEmpty())
            Text("No live games right now.", style = Cj.Body.copy(color = Cj.Dim), modifier = Modifier.padding(bottom = 6.dp))
        else
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                itemsIndexed(live, key = { _, g -> "live-" + g.id }) { _, g ->
                    GameCard(g, cfg.leagueNames[g.league] ?: g.league, cfg.follow, hide, onOk = {})
                }
            }

        if (upcoming.isNotEmpty()) {
            Text("Upcoming Games", style = Cj.RowTitle, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(upcoming, key = { _, g -> "up-" + g.id }) { _, g ->
                    GameCard(g, cfg.leagueNames[g.league] ?: g.league, cfg.follow, hide, onOk = {})
                }
            }
        }

        if (finals.isNotEmpty()) {
            Text("Final", style = Cj.RowTitle, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(finals, key = { _, g -> "fin-" + g.id }) { _, g ->
                    GameCard(g, cfg.leagueNames[g.league] ?: g.league, cfg.follow, hide, onOk = {})
                }
            }
        }
    }
}

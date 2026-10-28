package dev.couchtv.ontv.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo
import dev.couchtv.ontv.ui.components.HeroCarousel
import dev.couchtv.ontv.ui.components.PosterCard
import dev.couchtv.ontv.ui.components.ProviderTile
import dev.couchtv.ontv.ui.components.RailRow
import dev.couchtv.ontv.ui.components.ScorePill

/** Home: the hero carousel, Browse by Provider, and the trending rails, with the score pill afloat. */
@Composable
fun HomeScreen(
    games: List<UiRepo.Game>,
    follow: List<String>,
    onPlay: (String) -> Unit,
    onSeries: (UiRepo.RailItem) -> Unit,
    onProvider: (UiRepo.Provider) -> Unit,
    onSports: () -> Unit,
    onUp: (() -> Unit)? = null
) {
    var home by remember { mutableStateOf<UiRepo.HomeData?>(null) }
    var note by remember { mutableStateOf("Loading your line…") }
    LaunchedEffect(Unit) {
        runCatching { home = UiRepo.home() }.onFailure { note = it.message ?: "The line did not answer." }
    }

    Box(Modifier.fillMaxSize()) {
        val h = home
        if (h == null) {
            Text(note, style = Cj.Body, modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "hero") {
                    HeroCarousel(h.featured, onPlay = { onPlay(it.url) }, onUp = onUp,
                        modifier = Modifier.padding(horizontal = 0.dp))
                }
                item(key = "providers") {
                    RailRow("Browse by Provider", h.providers.size) { i ->
                        val p = h.providers[i]
                        ProviderTile(p.id, p.name, onOk = { onProvider(p) })
                    }
                }
                item(key = "movies") {
                    RailRow("Trending Movies", h.movies.size) { i ->
                        val m = h.movies[i]
                        PosterCard(m.name, m.logo, m.rating, onOk = { onPlay(m.url) })
                    }
                }
                item(key = "series") {
                    RailRow("Trending Series", h.series.size) { i ->
                        val s = h.series[i]
                        PosterCard(s.name, s.logo, s.rating, onOk = { onSeries(s) })
                    }
                }
                item { Box(Modifier.padding(bottom = 40.dp)) }
            }
        }
        ScorePill(
            games = games.filter { g -> g.state == "in" || g.home.abbr in follow || g.away.abbr in follow },
            follow = follow, onOk = onSports,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = 26.dp))
    }
}

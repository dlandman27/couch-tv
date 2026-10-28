package dev.couchtv.ontv.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.couchtv.ontv.PlayerActivity
import dev.couchtv.ontv.Store
import dev.couchtv.ontv.ui.components.EpisodesOverlay
import dev.couchtv.ontv.ui.components.NavBar
import dev.couchtv.ontv.ui.screens.AppsScreen
import dev.couchtv.ontv.ui.screens.BrowseScreen
import dev.couchtv.ontv.ui.screens.HomeScreen
import dev.couchtv.ontv.ui.screens.SearchScreen
import dev.couchtv.ontv.ui.screens.SportsScreen
import kotlinx.coroutines.delay
import org.json.JSONArray

/**
 * The whole TV experience: the glass nav on top, one screen below it, the series overlay above
 * everything. Thin by design - every screen and component lives in its own file.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CouchApp() {
    val ctx = LocalContext.current
    val tabs = listOf("Home", "Movies", "Shows", "Live TV", "Sports", "Search", "Apps")
    val tabFocus = remember { FocusRequester() }
    var tab by remember { mutableIntStateOf(0) }
    var provCat by remember { mutableStateOf<String?>(null) }   // a provider tile's category handoff
    var overlay by remember { mutableStateOf<Pair<String, List<UiRepo.Season>>?>(null) }
    var overlayNote by remember { mutableStateOf<String?>(null) }

    // who is watching, straight from the bridge's people list
    var people by remember { mutableStateOf(listOf("Person 1")) }
    var whoAt by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        runCatching {
            val arr = Store.people()
            people = (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }.ifEmpty { listOf("·") }
        }
    }

    // the scores feeding the pill and the Sports screen, refreshed on a live-aware cadence
    val cfg = remember { UiRepo.sportsConfig() }
    var games by remember { mutableStateOf<List<UiRepo.Game>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            runCatching { games = UiRepo.games(cfg.leagues) }
            delay(if (games.any { it.state == "in" }) 60_000 else 300_000)
        }
    }

    fun play(url: String) {
        if (url.isEmpty()) return
        ctx.startActivity(Intent(ctx, PlayerActivity::class.java).putExtra("url", url))
    }

    fun openSeries(item: UiRepo.RailItem) {
        overlay = item.name to emptyList()
        overlayNote = null
    }
    // the overlay's seasons load once it opens
    LaunchedEffect(overlay?.first) {
        val name = overlay?.first ?: return@LaunchedEffect
        if (overlay?.second?.isNotEmpty() == true) return@LaunchedEffect
        runCatching {
            val id = pendingSeriesId ?: return@LaunchedEffect
            val (n, seasons) = UiRepo.seasons(id)
            overlay = (if (n.isNotEmpty()) n else name) to seasons
        }.onFailure { overlayNote = it.message }
    }

    BackHandler(enabled = overlay != null || tab != 0) {
        if (overlay != null) overlay = null else tab = 0
    }

    Box(Modifier.fillMaxSize().background(Cj.GroundBrush)) {
        // The pages fill the whole screen; the nav floats over them, the reference design's way.
        // Home runs its hero under the glass; every other page starts below it.
        Box(Modifier.fillMaxSize()
                .padding(top = if (tab == 0) 0.dp else 92.dp)
                // leaving a page upward always lands on the open tab, never the avatar
                // (the properties must sit before the group they describe)
                .focusProperties {
                    exit = { dir -> if (dir == FocusDirection.Up) tabFocus else FocusRequester.Default }
                }
                .focusGroup()) {
                when (tab) {
                    0 -> HomeScreen(games, cfg.follow,
                        onPlay = ::play,
                        onSeries = { pendingSeriesId = it.id; openSeries(it) },
                        onProvider = { p ->
                            provCat = p.vodCat ?: p.seriesCat
                            tab = if (p.vodCat != null) 1 else 2
                        },
                        onSports = { tab = 4 },
                        onUp = { android.util.Log.i("couchtv.ui", "hero up -> nav"); runCatching { tabFocus.requestFocus() }.onFailure { android.util.Log.w("couchtv.ui", "nav focus: ${it.message}") } })
                    1 -> BrowseScreen("vod", provCat, onPlay = ::play, onSeries = { pendingSeriesId = it.id; openSeries(it) })
                    2 -> BrowseScreen("series", provCat, onPlay = ::play, onSeries = { pendingSeriesId = it.id; openSeries(it) })
                    3 -> BrowseScreen("live", null, onPlay = ::play, onSeries = {})
                    4 -> SportsScreen(cfg, games)
                    5 -> SearchScreen(onPlay = ::play, onSeries = { pendingSeriesId = it.id; openSeries(it) })
                    6 -> AppsScreen()
                }
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 40.dp, vertical = 14.dp)) {
            AsyncImage(model = "file:///android_asset/www/tv/orbit-logo.png", contentDescription = "orbit",
                contentScale = ContentScale.Fit, modifier = Modifier.height(30.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavBar(tabs, tab, who = people.getOrElse(whoAt) { "·" },
                    onPick = { tab = it; if (it != 1 && it != 2) provCat = null },
                    onWho = { whoAt = (whoAt + 1) % people.size },
                    tabFocus = tabFocus)
            }
            Box(Modifier.size(30.dp))
        }
        overlay?.let { (name, seasons) ->
            EpisodesOverlay(
                name = if (overlayNote != null) "$name — ${overlayNote}" else name,
                seasons = seasons,
                onPlay = { e -> play(e.url) })
        }
    }
}

/** The series whose overlay is loading; a plain var keeps the composable signatures small. */
private var pendingSeriesId: String? = null

package dev.couchtv.ontv.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.couchtv.ontv.PlayerActivity
import dev.couchtv.ontv.Store
import dev.couchtv.ontv.ui.components.DetailOverlay
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
    // Icon-only tabs in the pill; Search (index 6) lives on the right with the avatar.
    val tabIcons = NavIcons.tabs
    val tabNames = listOf("Home", "Movies", "Shows", "Live", "Sports", "Apps")
    val tabFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }   // Down from the header lands here
    var tab by remember { mutableIntStateOf(0) }
    var provCat by remember { mutableStateOf<String?>(null) }   // a provider tile's category handoff
    var searchQuery by rememberSaveable { mutableStateOf("") }   // kept across visits to search
    var overlay by remember { mutableStateOf<Pair<String, List<UiRepo.Season>>?>(null) }
    var overlayNote by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<Triple<String, String, String>?>(null) }   // kind, id, name

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
    // a poster opens its detail page; the hero's own Play still plays straight away
    fun openDetail(item: UiRepo.RailItem) { detail = Triple(if (item.kind == "series") "series" else "vod", item.id, item.name) }
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

    BackHandler(enabled = detail != null || overlay != null || tab != 0) {
        when {
            overlay != null -> overlay = null
            detail != null -> detail = null
            else -> tab = 0
        }
    }

    Box(Modifier.fillMaxSize()) {
        // The cinematic night wallpaper sits behind everything, dimmed to suit each screen:
        // visible on Home/Search, a faint glow behind the catalogue, near-black for the grids.
        val (wall, dim, blurBg) = when (tab) {
            0 -> Triple(true, 0.30f, false)      // Home — wallpaper present; Home's own gradient keeps text legible
            6 -> Triple(true, 0.42f, false)      // Search — the same scene, a little dimmer for the keyboard
            1, 2 -> Triple(true, 0.90f, false)   // Movies, Shows — faint artwork ambience
            4 -> Triple(true, 0.86f, false)      // Sports — restrained atmosphere
            5 -> Triple(true, 0.52f, true)       // Apps — wallpaper, softly blurred
            else -> Triple(false, 0f, false)     // Live TV — solid near-black
        }
        dev.couchtv.ontv.ui.components.AmbientBackground(wall, dim, blurBg)
        // The pages fill the whole screen; the nav floats over them, the reference design's way.
        // Home runs its hero under the glass; every other page starts below it.
        Box(Modifier.fillMaxSize()
                // the header floats over every page now; each screen insets its own top content
                // so nothing hides behind it, and the scroll areas pass up under it
                // leaving a page upward always lands on the open tab, never the avatar
                // (the properties must sit before the group they describe); the requester lets the
                // header send focus back down into the page (a focusGroup enters its first child)
                .focusRequester(contentFocus)
                .focusProperties {
                    exit = { dir -> if (dir == FocusDirection.Up) tabFocus else FocusRequester.Default }
                }
                .focusGroup()) {
              // Home and Search cross-fade (search grows out of Home's field); other tabs switch instantly
              AnimatedContent(targetState = tab, label = "tabs", transitionSpec = {
                  if ((initialState == 0 && targetState == 6) || (initialState == 6 && targetState == 0))
                      fadeIn(tween(260, delayMillis = 60)) togetherWith fadeOut(tween(180))
                  else EnterTransition.None togetherWith ExitTransition.None
              }) { shown ->
                when (shown) {
                    0 -> HomeScreen(games, cfg.follow,
                        onPlay = ::play,
                        onOpen = ::openDetail,
                        onProvider = { p ->
                            provCat = p.vodCat ?: p.seriesCat
                            tab = if (p.vodCat != null) 1 else 2
                        },
                        onSports = { tab = 4 },
                        onSearch = { tab = 6; provCat = null },
                        onAllApps = { tab = 5; provCat = null },
                        onUp = { runCatching { tabFocus.requestFocus() } })
                    1 -> BrowseScreen("vod", provCat, onPlay = ::play, onOpen = ::openDetail)
                    2 -> BrowseScreen("series", provCat, onPlay = ::play, onOpen = ::openDetail)
                    3 -> BrowseScreen("live", null, onPlay = ::play, onOpen = {})
                    4 -> SportsScreen(cfg, games)
                    5 -> AppsScreen()
                    6 -> SearchScreen(
                        query = searchQuery, onQuery = { searchQuery = it },
                        onPlay = ::play, onOpen = ::openDetail,
                        onSports = { tab = 4 }, onExit = { tab = 0 },
                        backEnabled = detail == null && overlay == null)
                }
              }
        }
        // a soft top scrim so the floating logo and avatar stay legible over bright hero art
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(74.dp).background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to Color(0xA6050505), 1f to Color.Transparent)))
        // logo on the left, the tab row centered on screen, search + account on the right
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 48.dp, vertical = 14.dp)) {
            AsyncImage(model = "file:///android_asset/www/tv/orbit-logo.png", contentDescription = "Orbit",
                contentScale = ContentScale.Fit, modifier = Modifier.align(Alignment.CenterStart).height(18.dp))
            NavBar(
                // search is part of Home, so Home stays the selected tab while searching
                icons = tabIcons, names = tabNames, selected = if (tab == 6) 0 else tab.coerceAtMost(5),
                onPick = { tab = it; if (it != 1 && it != 2) provCat = null },
                tabFocus = tabFocus, downTarget = contentFocus,
                modifier = Modifier.align(Alignment.Center))
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                // Home is already search-first, so the header search is only for the other screens
                if (tab != 0 && tab != 6) dev.couchtv.ontv.ui.components.NavIconButton(
                    NavIcons.Search, "Search", on = tab == 6,
                    onClick = { tab = 6; provCat = null },
                    idleContainer = Color.Transparent,
                    modifier = Modifier.focusProperties { down = contentFocus })
                // account avatar
                Surface(
                    onClick = { whoAt = (whoAt + 1) % people.size },
                    shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.CircleShape),
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                    border = ClickableSurfaceDefaults.border(focusedBorder = androidx.tv.material3.Border(
                        androidx.compose.foundation.BorderStroke(1.5.dp, Color.White), shape = androidx.compose.foundation.shape.CircleShape)),
                    modifier = Modifier.focusProperties { down = contentFocus }
                ) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(22.dp).background(
                            androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2A2A30), Color(0xFF131316))),
                            androidx.compose.foundation.shape.CircleShape)) {
                        androidx.tv.material3.Text(people.getOrElse(whoAt) { "·" }.take(1).uppercase(),
                            style = Cj.Small.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp))
                    }
                }
            }
        }
        detail?.let { (k, id, name) ->
            DetailOverlay(kind = k, id = id, fallbackName = name,
                onPlay = { url -> play(url) },
                onEpisodes = { pendingSeriesId = id; overlay = name to emptyList(); overlayNote = null },
                onOpen = ::openDetail,   // a "More like this" poster swaps the detail page
                onClose = { detail = null })
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

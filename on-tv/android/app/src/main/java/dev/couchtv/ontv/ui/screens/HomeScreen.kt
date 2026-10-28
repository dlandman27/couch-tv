package dev.couchtv.ontv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.couchtv.ontv.ui.moveKeys
import dev.couchtv.ontv.ui.rowEnds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.couchtv.ontv.Resume
import dev.couchtv.ontv.Tv
import dev.couchtv.ontv.ui.AppPrefs
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.NavIcons
import dev.couchtv.ontv.ui.Recents
import dev.couchtv.ontv.ui.UiRepo
import dev.couchtv.ontv.ui.components.AppIcon
import dev.couchtv.ontv.ui.components.ContinueCard
import dev.couchtv.ontv.ui.components.UniversalSearchBar

/**
 * Home: search-first and restrained. The centerpiece is a centered heading, the universal search
 * field and a quiet row of favorite apps, over the wallpaper. A compact Continue Watching rail sits
 * near the bottom as a secondary shortcut — only when something is actually resumable — never a
 * Netflix-style wall of recommendations. A soft middle band keeps the text legible.
 */
/** One Continue Watching card's data, read fresh each time Home resumes. */
private data class Resumable(val key: String, val name: String, val backdrop: String, val url: String,
                             val progress: Float, val remaining: String?)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    games: List<UiRepo.Game>,
    follow: List<String>,
    onPlay: (String) -> Unit,
    onOpen: (UiRepo.RailItem) -> Unit,
    onProvider: (UiRepo.Provider) -> Unit,
    onSports: () -> Unit,
    onSearch: () -> Unit = {},
    onAllApps: () -> Unit = {},
    onUp: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val allApps = remember {
        val pm = ctx.packageManager
        Tv.tvLaunchables().map { ri ->
            val icon = ri.activityInfo.loadIcon(pm) ?: ri.loadIcon(pm)
            Triple(ri.activityInfo.packageName, ri.loadLabel(pm).toString(), icon)
        }.filter { it.first != ctx.packageName }
    }
    val byPkg = allApps.associateBy { it.first }
    val allPkgs = allApps.map { it.first }
    // reorder mode: the app being slid along the row, and the order to go back to on Back
    var moving by remember { mutableStateOf<String?>(null) }
    var before by remember { mutableStateOf<List<String>>(emptyList()) }
    val ordered = AppPrefs.applyOrder(allApps.map { it.first }).filter { !AppPrefs.isHidden(it) }
    val favorites = (ordered.filter { AppPrefs.isPinned(it) }.ifEmpty { ordered }).mapNotNull { byPkg[it] }.take(8)

    // every return to Home (e.g. back from the player) bumps this, so progress and "N min left"
    // are re-read and focus can go back to the card that was playing
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) resumeTick++ }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    // only genuinely resumable titles: a saved progress mark (live channels have no duration, so
    // they never qualify; finished titles clear their mark). Newest first, a handful at most.
    val resumable = resumeTick.let {
        Recents.items.mapNotNull { r ->
            Resume.progress(r.url)?.let { p -> Resumable(r.id.ifEmpty { r.url }, r.name, r.backdrop, r.url, p, Resume.remaining(r.url)) }
        }.take(12)
    }
    val cardFocus = remember { mutableMapOf<String, FocusRequester>() }
    var lastPlayed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(resumeTick) {
        val key = lastPlayed ?: return@LaunchedEffect
        delay(80)   // let the rail lay out first
        runCatching { cardFocus[key]?.requestFocus() }
    }

    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { searchFocus.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        // a soft readability band: a little darker through the search zone and along the bottom for
        // the rail, while the wallpaper stays fully visible across the center and right
        // (shade behind the heading and search, clear through the middle of the scene, then a
        // deepening floor under the Continue Watching rail)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Transparent, 0.30f to Color(0x40000000), 0.42f to Color(0x2E000000),
            0.60f to Color.Transparent, 0.74f to Color(0x33000000), 1f to Color(0xA6000000))))

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.Start) {
            Spacer(Modifier.weight(1f))

            // the centerpiece — heading, search and the installed-app row, centered on screen
            Column(Modifier.fillMaxWidth().padding(horizontal = Cj.Gutter),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("What do you want to watch?",
                    style = androidx.compose.ui.text.TextStyle(fontFamily = Cj.Display, fontWeight = FontWeight.Normal,
                        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp,
                        color = Color.White.copy(alpha = 0.96f)))

                Spacer(Modifier.height(12.dp))
                UniversalSearchBar(onOk = onSearch, modifier = Modifier.focusRequester(searchFocus))

                if (favorites.isNotEmpty()) {
                    Spacer(Modifier.height(29.dp))   // + the ring's 3dp margin = the same 32dp gap
                    // tiles keep their 84px size; the wider columns give labels room, so the
                    // pitch (column + gap) is unchanged
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth().focusRestorer()) {
                        itemsIndexed(favorites, key = { _, a -> "f-" + a.first }) { i, (pkg, label, icon) ->
                            // hold OK to pick an app up; Left/Right slides it, OK places, Back cancels
                            AppIcon(label, icon, size = Cj.FavIcon, held = moving == pkg,
                                onOk = { if (moving == pkg) moving = null else runCatching { Tv.launchPackage(pkg) } },
                                onLongClick = { before = AppPrefs.order.toList(); moving = pkg },
                                modifier = Modifier
                                    .moveKeys(moving == pkg,
                                        onMove = { d -> AppPrefs.moveBy(allPkgs, favorites.map { it.first }, pkg, d) },
                                        onCancel = { AppPrefs.restore(before); moving = null })
                                    .rowEnds(first = i == 0, last = false))
                        }
                        // the row always ends in All Apps, the doorway to the full Apps screen
                        item(key = "all-apps") {
                            AppIcon("All Apps", null, size = Cj.FavIcon, glyph = NavIcons.Apps, onOk = onAllApps,
                                modifier = Modifier.rowEnds(first = false, last = true))
                        }
                    }
                    // reorder instructions: a fixed line under the row, shown only while an app is held
                    Box(Modifier.height(18.dp).padding(top = 6.dp), contentAlignment = Alignment.Center) {
                        if (moving != null) Text("Left/Right to move   ·   OK to place   ·   Back to cancel",
                            style = Cj.Small.copy(color = Color.White.copy(alpha = 0.5f), fontSize = 7.5.sp,
                                fontWeight = FontWeight.Normal, letterSpacing = 0.3.sp))
                    }
                }
            }

            Spacer(Modifier.weight(0.64f))

            // secondary shortcut: a Continue Watching rail anchored near the bottom, inside TV safe
            // margins. Hidden entirely when nothing is resumable, so Home stays search-first.
            if (resumable.isNotEmpty()) {
                Text("Continue Watching", style = Cj.Label.copy(color = Color.White.copy(alpha = 0.78f),
                    letterSpacing = 0.4.sp, fontSize = 11.sp),
                    modifier = Modifier.padding(start = Cj.Gutter, bottom = 9.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Cj.RailGap),
                    contentPadding = PaddingValues(start = Cj.Gutter, end = Cj.Gutter),
                    modifier = Modifier.fillMaxWidth().focusRestorer()) {
                    itemsIndexed(resumable, key = { _, r -> "cw-" + r.key }) { i, r ->
                        ContinueCard(
                            name = r.name,
                            image = r.backdrop,
                            remaining = r.remaining,
                            progress = r.progress,
                            modifier = Modifier.focusRequester(cardFocus.getOrPut(r.key) { FocusRequester() })
                                .rowEnds(first = i == 0, last = i == resumable.lastIndex),
                            // OK resumes straight away (the player seeks to the saved mark)
                            onOk = { lastPlayed = r.key; onPlay(r.url) })
                    }
                }
                Spacer(Modifier.height(27.dp))   // TV-safe bottom margin (54px)
            }
        }
    }
}

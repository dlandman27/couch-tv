package dev.couchtv.ontv.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import dev.couchtv.ontv.Tv
import dev.couchtv.ontv.search.Hit
import dev.couchtv.ontv.search.InstalledApp
import dev.couchtv.ontv.search.Kind
import dev.couchtv.ontv.search.QueryParser
import dev.couchtv.ontv.search.SearchEngine
import dev.couchtv.ontv.search.SearchHistory
import dev.couchtv.ontv.search.Section
import dev.couchtv.ontv.search.Shape
import dev.couchtv.ontv.search.VoiceInput
import dev.couchtv.ontv.ui.AppPrefs
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo
import dev.couchtv.ontv.ui.rowEnds
import dev.couchtv.ontv.ui.search.KindChip
import dev.couchtv.ontv.ui.search.ListeningPanel
import dev.couchtv.ontv.ui.search.ResultCard
import dev.couchtv.ontv.ui.search.SearchField
import dev.couchtv.ontv.ui.search.SearchIcons
import dev.couchtv.ontv.ui.search.SearchKeyboard
import dev.couchtv.ontv.ui.search.SearchSpec
import dev.couchtv.ontv.ui.search.SuggestionRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Universal search, Spotlight meets Apple TV. The field rises from Home's position to the top;
 * the remote keyboard sits on the left with live title suggestions under it; the right side
 * shows starter queries when empty, then grouped results — films, shows, live channels, today's
 * games and installed apps — as each source answers. Typed and spoken queries share all of it.
 * Logic (parsing, providers, voice) lives in dev.couchtv.ontv.search; this file only draws.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    query: String,
    onQuery: (String) -> Unit,
    onPlay: (String) -> Unit,
    onOpen: (UiRepo.RailItem) -> Unit,
    onSports: () -> Unit,
    onExit: () -> Unit,
    backEnabled: Boolean = true
) {
    val ctx = LocalContext.current
    val kbFocus = remember { FocusRequester() }
    val micFocus = remember { FocusRequester() }
    val resultsFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }
    var kbHasFocus by remember { mutableStateOf(false) }

    // the installed apps, as Home lists them, and the first favorite for a starter query
    val installed = remember {
        val pm = ctx.packageManager
        Tv.tvLaunchables().map { ri ->
            InstalledApp(ri.activityInfo.packageName, ri.loadLabel(pm).toString(), ri.activityInfo.loadIcon(pm) ?: ri.loadIcon(pm))
        }.filter { it.pkg != ctx.packageName }
    }
    val firstApp = remember {
        val order = AppPrefs.applyOrder(installed.map { it.pkg }).filter { !AppPrefs.isHidden(it) }
        (order.firstOrNull { AppPrefs.isPinned(it) } ?: order.firstOrNull())?.let { p -> installed.first { it.pkg == p }.label }
    }
    var starters by remember { mutableStateOf<List<String>>(emptyList()) }
    val recent = remember { SearchHistory.recent() }
    LaunchedEffect(Unit) {
        launch { runCatching { SearchEngine.prepare() } }   // build the catalogue index before the first keystroke
        starters = runCatching { SearchEngine.starters(UiRepo.sportsConfig().leagues, firstApp) }.getOrDefault(emptyList())
    }

    // ---- search: each provider fills its own slot as it answers ----
    val parsed = remember(query) { QueryParser.parse(query) }
    var titles by remember { mutableStateOf<List<Section>>(emptyList()) }
    var channels by remember { mutableStateOf<Section?>(null) }
    var games by remember { mutableStateOf<Section?>(null) }
    var apps by remember { mutableStateOf<Section?>(null) }
    var pending by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }   // a source didn't answer (still loading, offline)
    var filter by remember { mutableStateOf<Kind?>(null) }
    LaunchedEffect(query) {
        filter = null; failed = false
        if (query.isBlank()) { titles = emptyList(); channels = null; games = null; apps = null; pending = 0; return@LaunchedEffect }
        apps = SearchEngine.apps(parsed, installed)
        delay(260)   // let a fast typist finish the word
        // one source: true if it failed (not merely found nothing). A newer keystroke's cancellation
        // passes straight through, so a superseded search never counts as a failure.
        suspend fun <T> source(fetch: suspend () -> T, keep: (T) -> Unit): Boolean =
            try { keep(fetch()); false } catch (e: CancellationException) { throw e } catch (e: Exception) { true }
        // one pass over every source; true if any of them failed
        suspend fun pass(): Boolean {
            var bad = false
            pending = 3
            coroutineScope {
                launch { if (source({ SearchEngine.titles(parsed) }, { titles = it })) bad = true; pending-- }
                launch { if (source({ SearchEngine.channels(parsed) }, { channels = it })) bad = true; pending-- }
                launch { if (source({ SearchEngine.games(parsed) }, { games = it })) bad = true; pending-- }
            }
            return bad
        }
        if (pass()) { failed = true; delay(2500); failed = pass() }   // the catalogue may still be loading: retry once
    }
    val sections = SearchEngine.arrange(parsed, titles + listOfNotNull(games, channels, apps))
    val kindsPresent = sections.flatMap { s -> s.hits.map { it.kind } }.distinct().sortedBy { it.ordinal }

    // ---- voice: the same results, a quieter way in ----
    val voice = remember { VoiceInput(ctx) }
    DisposableEffect(Unit) { onDispose { voice.cancel() } }
    fun listen() = voice.start { text -> onQuery(text); SearchHistory.add(text) }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) listen() }
    fun mic() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
        else askMic.launch(Manifest.permission.RECORD_AUDIO)
    }
    val voiceShowing = voice.phase != VoiceInput.Phase.Idle || voice.message.isNotEmpty()
    LaunchedEffect(voiceShowing) {
        if (voiceShowing) { delay(60); runCatching { cancelFocus.requestFocus() } }
    }

    fun open(h: Hit) {
        SearchHistory.add(query)
        when (h.kind) {
            Kind.Movie, Kind.Show -> onOpen(UiRepo.RailItem(h.id, if (h.kind == Kind.Show) "series" else "vod",
                h.title, "", h.image, 0.0, h.url))
            Kind.Channel -> onPlay(h.url)
            Kind.Game -> onSports()
            Kind.App -> runCatching { Tv.launchPackage(h.pkg) }
        }
    }

    // Back: voice → stop listening; results/mic → back to the keyboard's last key; keyboard → leave search
    BackHandler(enabled = backEnabled) {
        when {
            voiceShowing -> { voice.cancel(); voice.dismissMessage(); runCatching { kbFocus.requestFocus() } }
            !kbHasFocus -> runCatching { kbFocus.requestFocus() }
            else -> onExit()
        }
    }

    // the field rises from where Home's sits; the rest fades in under it
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        runCatching { kbFocus.requestFocus() }
        enter.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
    }

    Box(Modifier.fillMaxSize()) {
        // a light extra veil so the keyboard and posters read over the wallpaper
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color(0x33000000), 0.35f to Color(0x47000000), 1f to Color(0x8C000000))))

        Column(Modifier.fillMaxSize().padding(top = SearchSpec.FieldTop)) {
            SearchField(query, voice.phase == VoiceInput.Phase.Listening, voice.heard, onMic = ::mic, micFocus = micFocus,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .graphicsLayer { translationY = (1f - enter.value) * 100.dp.toPx() })

            // two columns across the safe area: keyboard + suggestions (28%), a gap, results (68%)
            Row(Modifier.fillMaxSize().padding(start = Cj.Gutter, end = Cj.Gutter, top = 20.dp)
                .graphicsLayer { alpha = enter.value }) {
                // LEFT — the keyboard, then live title suggestions while typing
                Column(Modifier.weight(SearchSpec.KeyboardShare)) {
                    SearchKeyboard(
                        onChar = { onQuery(query + it) },
                        onBackspace = { if (query.isNotEmpty()) onQuery(query.dropLast(1)) },
                        onSpace = { if (query.isNotEmpty() && !query.endsWith(" ")) onQuery("$query ") },
                        onClear = { onQuery("") },
                        onSearch = { SearchHistory.add(query); if (sections.isNotEmpty()) runCatching { resultsFocus.requestFocus() } },
                        keyboardFocus = kbFocus,
                        modifier = Modifier.onFocusChanged { kbHasFocus = it.hasFocus })
                    val suggestions = if (query.isBlank() || parsed.structured) emptyList()
                        else sections.flatMap { it.hits }.filter { it.kind == Kind.Movie || it.kind == Kind.Show }
                            .map { it.title }.distinct().filter { !it.equals(query, true) }.take(3)
                    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        suggestions.forEach { s ->
                            SuggestionRow(s, SearchIcons.Arrow, onOk = { onQuery(s.lowercase()); SearchHistory.add(s) },
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Spacer(Modifier.weight(SearchSpec.GapShare))

                // RIGHT — starters, listening, or results, out to the right safe margin
                Box(Modifier.weight(SearchSpec.ResultsShare).fillMaxSize()) {
                    when {
                        voiceShowing -> ListeningPanel(voice.heard, voice.level, voice.message,
                            onCancel = { voice.cancel(); voice.dismissMessage(); runCatching { kbFocus.requestFocus() } },
                            cancelFocus = cancelFocus, modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp))
                        query.isBlank() -> Starters(starters, recent, onPick = { onQuery(it.lowercase()) })
                        else -> Results(
                            sections = sections, kinds = kindsPresent, filter = filter, onFilter = { filter = it },
                            summary = parsed.summary(), searching = pending > 0, failed = failed, query = query,
                            onOk = ::open, focus = resultsFocus)
                    }
                }
            }
        }
    }
}

/** The empty state: real starter queries from this TV, then the viewer's recent searches. */
@Composable
private fun Starters(starters: List<String>, recent: List<String>, onPick: (String) -> Unit) {
    Column(Modifier.width(300.dp).focusGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (starters.isNotEmpty()) {
            Caption("Try searching for")
            starters.forEach { SuggestionRow(it, SearchIcons.Sparkle, onOk = { onPick(it) }) }
        }
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Caption("Recent")
            recent.forEach { SuggestionRow(it, SearchIcons.History, onOk = { onPick(it) }) }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Results(
    sections: List<Section>, kinds: List<Kind>, filter: Kind?, onFilter: (Kind?) -> Unit,
    summary: String?, searching: Boolean, failed: Boolean, query: String, onOk: (Hit) -> Unit, focus: FocusRequester
) {
    Column(Modifier.fillMaxSize()) {
        // what the query was understood as — structure, not a chat reply
        if (summary != null) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(SearchIcons.Sparkle, contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(10.dp))
            Text(summary, style = Cj.Small.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp),
                modifier = Modifier.padding(start = 6.dp))
        }
        if (kinds.size >= 2) Row(Modifier.padding(bottom = 10.dp).focusRestorer(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KindChip("All", filter == null, onOk = { onFilter(null) })
            kinds.forEach { k -> KindChip(k.label, filter == k, onOk = { onFilter(k) }) }
        }
        when {
            sections.isEmpty() && searching -> Caption("Searching…")
            sections.isEmpty() && failed -> Caption("Still loading your catalog. Try again in a moment.")
            sections.isEmpty() -> Caption("No results for “$query”")
            else -> {
                // one five-column grid for everything; each group starts on its own row under a
                // full-width header, so every row holds one shape and every column is the same width.
                // A filter shows just that kind, headerless.
                val groups = if (filter == null) sections
                    else listOf(Section("", filter, sections.flatMap { it.hits }.filter { it.kind == filter }.distinctBy { it.key }))
                // results land group by group; keep the grid at the top while the viewer is still on
                // the keyboard, so a group arriving above doesn't push the others out of view
                val gridState = rememberLazyGridState()
                var browsing by remember { mutableStateOf(false) }
                LaunchedEffect(groups.map { it.title + it.hits.size }) { if (!browsing) gridState.scrollToItem(0) }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(SearchSpec.Columns),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 40.dp),   // the last row clears the screen edge
                    modifier = Modifier.fillMaxSize().focusRequester(focus).focusRestorer()
                        .onFocusChanged { browsing = it.hasFocus }
                ) {
                    groups.forEach { g ->
                        if (g.title.isNotEmpty()) item(key = "h:" + g.title, span = { GridItemSpan(maxLineSpan) }) {
                            Caption(g.title, Modifier.padding(top = 4.dp))
                        }
                        items(g.hits, key = { g.title + it.key }) { h -> ResultCard(h, onOk = { onOk(h) }, fill = true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Caption(t: String, modifier: Modifier = Modifier) = Text(t, style = Cj.Label.copy(color = Color.White.copy(alpha = 0.66f), fontSize = 10.5.sp),
    modifier = modifier.padding(bottom = 3.dp))

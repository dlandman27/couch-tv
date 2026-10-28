package dev.couchtv.ontv.search

import dev.couchtv.ontv.Live
import dev.couchtv.ontv.Scores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** How a result's artwork is shaped on screen. */
enum class Shape { Poster, Wide, Square }

/** One search result. Everything here comes from the IPTV line, the scoreboard or the package manager. */
data class Hit(
    val kind: Kind,
    val key: String,
    val title: String,
    val subtitle: String,
    val image: String = "",
    val image2: String = "",          // a game's home-team logo beside the away team's
    val icon: Any? = null,            // an installed app's launcher drawable
    val shape: Shape,
    val badge: String? = null,        // "LIVE"
    val id: String = "",
    val url: String = "",
    val pkg: String = "",
    val league: String = ""
)

data class Section(val title: String, val kind: Kind, val hits: List<Hit>)

/** An installed TV app, as the home screen already lists them. */
data class InstalledApp(val pkg: String, val label: String, val icon: Any?)

/**
 * Runs a [SearchQuery] against the real sources, one provider per kind, so the screen can show
 * each group the moment it arrives. No result is ever invented: an empty provider returns nothing.
 */
object SearchEngine {

    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

    /** Build the catalogue index before the first keystroke, so typing never races it. */
    suspend fun prepare() = withContext(Dispatchers.IO) { Live.warmSearch() }

    /** Films and shows: name words, genre (via the line's genre categories) and year combined. */
    suspend fun titles(q: SearchQuery): List<Section> = withContext(Dispatchers.IO) {
        val wantMovie = q.kinds.isEmpty() || Kind.Movie in q.kinds
        val wantShow = q.kinds.isEmpty() || Kind.Show in q.kinds
        if (q.openApp != null || (!wantMovie && !wantShow)) return@withContext emptyList()
        if (!q.structured && q.text.length < 2) return@withContext emptyList()
        val kinds = buildSet { if (wantMovie) add("vod"); if (wantShow) add("series") }
        val cats = q.genres.flatMap { QueryParser.catTokens(it) }
        val found = Live.find(kinds, q.words, cats, emptyList(), q.years, 60).objects()
        fun hit(o: JSONObject, k: Kind) = Hit(k, k.name + o.optString("id"), o.optString("name"),
            listOf(o.optString("year"), if (k == Kind.Show) "Series" else "Movie").filter { it.isNotEmpty() }.joinToString(" · "),
            image = o.optString("logo"), shape = Shape.Poster, id = o.optString("id"), url = o.optString("url"))
        val movies = found.filter { it.optString("kind") == "vod" }.map { hit(it, Kind.Movie) }
        val shows = found.filter { it.optString("kind") == "series" }.map { hit(it, Kind.Show) }
        val label = q.genres.joinToString(" & ") { QueryParser.genreLabel(it) }.let { if (it.isEmpty()) "" else "$it " }
        listOfNotNull(
            movies.takeIf { it.isNotEmpty() }?.let { Section("${label}Movies", Kind.Movie, it) },
            shows.takeIf { it.isNotEmpty() }?.let { Section("${label}Shows", Kind.Show, it) })
    }

    /** Live channels: by name, by sports categories, by network, and the league's own channels. */
    suspend fun channels(q: SearchQuery): Section? = withContext(Dispatchers.IO) {
        if (q.openApp != null) return@withContext null
        val wanted = q.kinds.isEmpty() || Kind.Channel in q.kinds || q.networks.isNotEmpty() || q.leagues.isNotEmpty()
        if (!wanted) return@withContext null
        val hits = LinkedHashMap<String, JSONObject>()
        fun add(arr: JSONArray) = arr.objects().forEach { hits.putIfAbsent(it.optString("id"), it) }
        when {
            q.sportsChannels -> add(Live.find(setOf("live"), q.words, listOf("sport"), emptyList(), null, 60))
            q.networks.isNotEmpty() -> add(Live.find(setOf("live"), emptyList(), emptyList(), q.networks, null, 30))
            q.leagues.isNotEmpty() -> add(Live.find(setOf("live"), emptyList(), emptyList(),
                q.leagues.map { Scores.leagueName(it).lowercase() }, null, 20))
            Kind.Channel in q.kinds -> add(Live.find(setOf("live"), q.words, emptyList(), emptyList(), null, 60))
            !q.structured && q.text.length >= 2 -> add(Live.find(setOf("live"), q.words, emptyList(), emptyList(), null, 30))
        }
        if (hits.isEmpty()) return@withContext null
        Section(if (q.sportsChannels) "Sports Channels" else "Live TV", Kind.Channel, hits.values.map { o ->
            Hit(Kind.Channel, "ch" + o.optString("id"), o.optString("name"), "Live channel",
                image = o.optString("logo"), shape = Shape.Wide, id = o.optString("id"), url = o.optString("url"))
        })
    }

    /** Today's games from the scoreboard: by team, league and network. Live first, finals last. */
    suspend fun games(q: SearchQuery): Section? = withContext(Dispatchers.IO) {
        if (q.openApp != null || q.sportsChannels) return@withContext null
        val asked = Kind.Game in q.kinds || q.leagues.isNotEmpty()
        // a plain word might be a team ("rangers"); only look when it's long enough to mean something
        if (!asked && (q.structured || q.text.length < 4)) return@withContext null
        val leagues = q.leagues.ifEmpty { Scores.allLeagues() }
        val games = (Scores.get(leagues).optJSONArray("games") ?: JSONArray()).objects()
        val words = q.words.map { it.lowercase() }
        val matched = games.filter { g ->
            val teams = listOf("home", "away").flatMap { s ->
                (g.optJSONObject(s) ?: JSONObject()).let { listOf(it.optString("name"), it.optString("full"), it.optString("abbr")) }
            }.joinToString(" ").lowercase()
            val nets = g.optJSONArray("networks")?.let { a -> (0 until a.length()).map { a.optString(it).lowercase() } } ?: emptyList()
            (words.isEmpty() || words.all { teams.contains(it) }) &&
                (q.networks.isEmpty() || q.networks.any { n -> nets.any { it.contains(n) } }) &&
                (!q.liveOnly || g.optString("state") != "post")
        }.sortedBy { when (it.optString("state")) { "in" -> 0; "pre" -> 1; else -> 2 } }
        if (matched.isEmpty()) return@withContext null
        Section(if (matched.any { it.optString("state") == "in" }) "Live Sports" else "Games", Kind.Game, matched.map { g ->
            val home = g.optJSONObject("home") ?: JSONObject(); val away = g.optJSONObject("away") ?: JSONObject()
            val live = g.optString("state") == "in"
            val nets = g.optJSONArray("networks")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
            val score = if (g.optString("state") != "pre") "${away.optString("score")}–${home.optString("score")}" else ""
            Hit(Kind.Game, "g" + g.optString("id"), "${away.optString("name")} vs ${home.optString("name")}",
                listOf(Scores.leagueName(g.optString("league")), nets.firstOrNull() ?: "", score, g.optString("detail"))
                    .filter { it.isNotEmpty() }.joinToString(" · "),
                image = away.optString("logo"), image2 = home.optString("logo"), shape = Shape.Wide,
                badge = if (live) "LIVE" else null, id = g.optString("id"), league = g.optString("league"))
        })
    }

    /** Installed apps whose name matches, or the one an "open …" asked for. */
    fun apps(q: SearchQuery, installed: List<InstalledApp>): Section? {
        val want = (q.openApp ?: if (!q.structured || Kind.App in q.kinds) q.text else null)?.lowercase()?.trim()
        if (want.isNullOrEmpty() || want.length < 2) return null
        val compact = want.replace(" ", "")
        val hits = installed.filter { a ->
            val l = a.label.lowercase()
            l.contains(want) || l.replace(" ", "").contains(compact) || (want.length >= 4 && compact.contains(l.replace(" ", "")))
        }.sortedBy { if (it.label.lowercase().startsWith(want)) 0 else 1 }
        if (hits.isEmpty()) return null
        return Section("Apps", Kind.App, hits.map { a ->
            Hit(Kind.App, "app" + a.pkg, a.label, "App", icon = a.icon, shape = Shape.Square, pkg = a.pkg)
        })
    }

    /**
     * Orders the groups for this query: what was asked for first ("open youtube" → apps, "the
     * Islanders game" → games), and for plain text, exact-title matches ahead of loose ones.
     */
    fun arrange(q: SearchQuery, sections: List<Section>): List<Section> {
        val rank: (Kind) -> Int = when {
            q.openApp != null -> { k -> if (k == Kind.App) 0 else 5 }
            q.sportsChannels -> { k -> if (k == Kind.Channel) 0 else 5 }
            Kind.Game in q.kinds || q.leagues.isNotEmpty() -> { k -> when (k) { Kind.Game -> 0; Kind.Channel -> 1; else -> 5 } }
            q.networks.isNotEmpty() -> { k -> when (k) { Kind.Channel -> 0; Kind.Game -> 1; else -> 5 } }
            else -> { k -> when (k) { Kind.Movie -> 0; Kind.Show -> 1; Kind.App -> 2; Kind.Game -> 3; Kind.Channel -> 4 } }
        }
        val ordered = sections.filter { it.hits.isNotEmpty() }.sortedBy { rank(it.kind) }
        if (q.structured) return ordered
        // plain text: lead with a mixed "Top Results" of titles that start with what was typed
        val t = q.text.lowercase()
        // Top Results are films and shows only, so the row keeps one poster shape
        val top = ordered.filter { it.kind == Kind.Movie || it.kind == Kind.Show }
            .flatMap { s -> s.hits.take(6) }.filter { it.title.lowercase().startsWith(t) }.take(5)
        if (top.size < 2) return ordered
        val topKeys = top.map { it.key }.toSet()
        return listOf(Section("Top Results", top.first().kind, top)) +
            ordered.map { it.copy(hits = it.hits.filter { h -> h.key !in topKeys }) }.filter { it.hits.isNotEmpty() }
    }

    /**
     * Starter queries for the empty state, built from what this TV really has: the line's own
     * genre categories, the followed leagues, a sports channel group, and the first favorite app.
     */
    suspend fun starters(leagues: List<String>, firstApp: String?): List<String> = withContext(Dispatchers.IO) {
        val out = mutableListOf<String>()
        val genreCats = runCatching { Live.genreNames("vod") }.getOrDefault(emptyList()).map { it.lowercase() }
        listOf("action", "comedy", "horror", "sci-fi", "thriller", "documentary").filter { g ->
            QueryParser.catTokens(g).any { t -> genreCats.any { it.contains(t) } }
        }.take(2).forEach { out += "${QueryParser.genreLabel(it)} movies" }
        leagues.firstOrNull()?.let { out += "${Scores.leagueName(it)} games today" }
        if (runCatching { Live.genreNames("live") }.getOrDefault(emptyList()).any { it.lowercase().contains("sport") })
            out += "Live sports channels"
        firstApp?.let { out += "Open $it" }
        out
    }
}

package dev.couchtv.ontv.search

import java.util.Calendar

/** What a result is. The UI groups and filters by this. */
enum class Kind(val label: String) { Movie("Movies"), Show("Shows"), Channel("Live TV"), Game("Sports"), App("Apps") }

/**
 * A query turned into structure. Typed or spoken text goes through [QueryParser.parse]; anything
 * it can't place stays in [words] and is searched by name, so plain text always still works.
 */
data class SearchQuery(
    val raw: String,
    val words: List<String>,          // left-over words, matched against names
    val kinds: Set<Kind>,             // empty = every kind
    val genres: List<String>,         // canonical genre keys ("horror", "sci-fi")
    val years: IntRange?,
    val leagues: List<String>,        // Scores league ids ("nhl")
    val networks: List<String>,       // "espn"
    val openApp: String?,             // "open YouTube" → "youtube"
    val sportsChannels: Boolean,      // "live sports channels"
    val liveOnly: Boolean             // "on now", "live", "tonight"
) {
    /** True when the parser found intent beyond plain words. */
    val structured get() = kinds.isNotEmpty() || genres.isNotEmpty() || years != null ||
        leagues.isNotEmpty() || networks.isNotEmpty() || openApp != null || sportsChannels
    val text get() = words.joinToString(" ")

    /** A short human reading of the structure ("Horror · Movies · 2010s"), shown above results. */
    fun summary(): String? {
        if (!structured) return null
        val parts = mutableListOf<String>()
        openApp?.let { return "Open app" }
        if (sportsChannels) parts += "Sports channels"
        genres.forEach { g -> parts += QueryParser.genreLabel(g) }
        leagues.forEach { parts += dev.couchtv.ontv.Scores.leagueName(it) }
        if (!sportsChannels) kinds.forEach { parts += it.label }
        years?.let { parts += if (it.first == it.last) "${it.first}" else if (it.last - it.first == 9) "${it.first}s" else "${it.first}–${it.last}" }
        networks.forEach { parts += "on " + it.uppercase() }
        if (words.isNotEmpty()) parts += "“${text}”"
        return parts.joinToString("  ·  ")
    }
}

/**
 * Natural language to [SearchQuery], with rules rather than a model: predictable, instant, works
 * offline and can't make things up. "Show me horror movies for Halloween" → genre horror, kind
 * movie. "Find sci-fi movies from the 2010s" → sci-fi, movie, 2010–2019. "Find the Islanders
 * game" → kind game, words [islanders]. "Show me NBA games on ESPN" → league nba, network espn.
 * "Open YouTube" → app. Unrecognised phrasing simply becomes words.
 */
object QueryParser {

    // canonical genre → words that ask for it (also what's looked for in category names)
    private val GENRES = linkedMapOf(
        "horror" to listOf("horror", "scary", "halloween", "spooky", "slasher"),
        "sci-fi" to listOf("sci-fi", "scifi", "sci fi", "science fiction", "science-fiction"),
        "comedy" to listOf("comedy", "comedies", "funny"),
        "action" to listOf("action"),
        "thriller" to listOf("thriller", "thrillers", "suspense"),
        "drama" to listOf("drama", "dramas"),
        "romance" to listOf("romance", "romantic", "rom-com", "romcom"),
        "documentary" to listOf("documentary", "documentaries", "docuseries"),
        "animation" to listOf("animation", "animated", "cartoon", "cartoons"),
        "kids" to listOf("kids", "children", "family"),
        "crime" to listOf("crime"),
        "mystery" to listOf("mystery", "mysteries"),
        "fantasy" to listOf("fantasy"),
        "adventure" to listOf("adventure", "adventures"),
        "war" to listOf("war"),
        "western" to listOf("western", "westerns"),
        "anime" to listOf("anime"),
    )
    // canonical genre → substrings to look for in an IPTV category's name
    private val GENRE_CATS = mapOf(
        "sci-fi" to listOf("sci-fi", "sci fi", "scifi", "science fiction", "sf"),
        "kids" to listOf("kids", "family", "children"),
        "animation" to listOf("animation", "animated", "cartoon"),
        "documentary" to listOf("documentar"),
        "romance" to listOf("romance", "romantic"),
    )
    fun catTokens(genre: String) = GENRE_CATS[genre] ?: listOf(genre)
    fun genreLabel(g: String) = when (g) { "sci-fi" -> "Sci-Fi"; else -> g.replaceFirstChar { it.uppercase() } }

    private val LEAGUES = linkedMapOf(
        "nhl" to listOf("nhl", "hockey"),
        "nba" to listOf("nba", "basketball"),
        "nfl" to listOf("nfl", "football"),
        "mlb" to listOf("mlb", "baseball"),
        "mls" to listOf("mls"),
        "ncaaf" to listOf("college football", "ncaaf", "ncaa football"),
        "eng.1" to listOf("premier league", "epl"),
        "uefa.champions" to listOf("champions league", "ucl"),
    )
    // networks a person names after "on" ("NBA games on ESPN")
    private val NETWORKS = listOf("espn", "abc", "nbc", "cbs", "fox", "tnt", "tbs", "msg", "sky sports", "sky",
        "bein", "dazn", "nba tv", "nfl network", "nhl network", "mlb network", "peacock", "prime video", "tsn", "sportsnet")
    private val SPORTS_WORDS = listOf("sports", "sport")

    // phrasing that carries no meaning for search
    private val FILLER = listOf(
        "can you", "could you", "please", "i want to watch", "i wanna watch", "i want", "show me", "show",
        "find me", "find", "search for", "search", "look for", "get me", "give me", "play", "watch", "put on",
        "some", "any", "all", "the", "a", "an", "me", "for", "of", "to", "from", "in", "with", "that", "are", "is",
        "good", "best", "great", "top", "popular", "today", "tonight", "now", "on", "my")

    private val KIND_WORDS = listOf(
        Kind.Movie to listOf("movies", "movie", "films", "film"),
        Kind.Show to listOf("tv shows", "tv show", "shows", "show", "series", "episodes"),
        Kind.Game to listOf("games", "game", "matches", "match", "scores"),
        Kind.Channel to listOf("channels", "channel", "live tv", "stations"),
        Kind.App to listOf("apps", "app"),
    )

    fun parse(rawIn: String): SearchQuery {
        val raw = rawIn.trim()
        var s = " " + raw.lowercase().replace(Regex("[?!.,]"), " ").replace(Regex("\\s+"), " ").trim() + " "

        // "open youtube" / "launch netflix" → an app, nothing else to parse
        Regex("^ (?:open|launch|start|go to|run) (.+?) (?:app )?$").find(s)?.let { m ->
            val app = m.groupValues[1].removePrefix("the ").trim()
            return SearchQuery(raw, emptyList(), setOf(Kind.App), emptyList(), null, emptyList(), emptyList(),
                app, sportsChannels = false, liveOnly = false)
        }

        fun take(phrase: String): Boolean {
            val p = " $phrase "
            if (!s.contains(p)) return false
            s = s.replaceFirst(p, " "); return true
        }
        // consume every synonym present (not just the first), so none is left behind as a name word
        fun takeAny(phrases: List<String>) = phrases.map { take(it) }.any { it }

        // a leading command ("show me", "find") is phrasing, not a request for TV shows
        s = s.replace(Regex("^ (?:(?:can|could) you |please )?(?:show me|find me|find|search for|look for|get me|give me|i want to watch|i wanna watch|play|watch|put on) "), " ")

        val liveOnly = takeAny(listOf("live now", "on now", "playing now", "tonight", "live"))

        // "on espn" — networks, read before kinds so "nba tv" isn't eaten as "tv"
        val networks = NETWORKS.filter { take("on $it") || take(it) }

        val leagues = LEAGUES.filter { (_, words) -> takeAny(words) }.keys.toList()

        val genres = GENRES.filter { (_, words) -> takeAny(words) }.keys.toList()

        // years: "2010s", "the 90s", "80's", "from 2015", "2015"
        var years: IntRange? = null
        Regex(" (?:the )?((?:19|20)?\\d0)'?s ").find(s)?.let { m ->
            var start = m.groupValues[1].toInt()
            if (start < 100) start += if (start >= 30) 1900 else 2000
            years = start..start + 9; s = s.replaceFirst(m.value, " ")
        }
        if (years == null) Regex(" (?:from |in |of )?((?:19|20)\\d{2}) ").find(s)?.let { m ->
            val y = m.groupValues[1].toInt()
            if (y in 1920..Calendar.getInstance().get(Calendar.YEAR) + 1) { years = y..y; s = s.replaceFirst(m.value, " ") }
        }
        if (years == null && (take("new") || take("latest") || take("recent"))) {
            val now = Calendar.getInstance().get(Calendar.YEAR); years = now - 1..now
        }

        val sportsWord = takeAny(SPORTS_WORDS)
        val kinds = KIND_WORDS.filter { (_, words) -> takeAny(words) }.map { it.first }.toMutableSet()
        // "sports channels" / "live sports" → channels in sports categories
        val sportsChannels = sportsWord && (Kind.Channel in kinds || kinds.isEmpty() && leagues.isEmpty())
        if (sportsChannels) kinds += Kind.Channel
        if (leagues.isNotEmpty() && kinds.isEmpty()) kinds += Kind.Game
        if (sportsWord && !sportsChannels && kinds.isEmpty()) kinds += Kind.Game
        // a genre or year with no kind means films and shows
        if (kinds.isEmpty() && (genres.isNotEmpty() || years != null)) { kinds += Kind.Movie; kinds += Kind.Show }

        // what's left, minus filler, is a name to look for ("islanders", "the last of us")
        var rest = s
        repeat(3) { FILLER.forEach { f -> rest = rest.replace(" $f ", " ") } }
        val words = rest.trim().split(" ").filter { it.isNotBlank() }
        // nothing structured at all → plain text search over the whole query as typed
        val structuredFound = kinds.isNotEmpty() || genres.isNotEmpty() || years != null || leagues.isNotEmpty() ||
            networks.isNotEmpty() || sportsChannels
        return if (!structuredFound) SearchQuery(raw, raw.lowercase().split(" ").filter { it.isNotBlank() },
            emptySet(), emptyList(), null, emptyList(), emptyList(), null, false, liveOnly = false)
        else SearchQuery(raw, words, kinds, genres, years, leagues, networks, null, sportsChannels, liveOnly)
    }
}

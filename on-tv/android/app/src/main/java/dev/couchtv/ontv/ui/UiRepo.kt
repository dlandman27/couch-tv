package dev.couchtv.ontv.ui

import dev.couchtv.ontv.Live
import dev.couchtv.ontv.Scores
import dev.couchtv.ontv.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The native screens' view of the data layer: Live, Scores and Store called in-process (no HTTP),
 * off the main thread, mapped to small immutable models. Each screen asks for exactly what it
 * draws; caching stays where it already lives, in Live and Scores.
 */
object UiRepo {

    // ---------------- models ----------------
    data class Featured(val id: String, val name: String, val year: String, val backdrop: String,
                        val poster: String, val plot: String, val genre: String, val rating: Double, val url: String)
    data class RailItem(val id: String, val kind: String, val name: String, val year: String,
                        val logo: String, val rating: Double, val url: String)
    data class Provider(val id: String, val name: String, val vodCat: String?, val seriesCat: String?)
    data class HomeData(val featured: List<Featured>, val movies: List<RailItem>,
                        val series: List<RailItem>, val providers: List<Provider>)

    data class Category(val id: String, val name: String)
    data class Episode(val id: String, val num: Int, val title: String, val url: String)
    data class Season(val season: Int, val episodes: List<Episode>)

    data class Side(val abbr: String, val name: String, val score: String, val logo: String)
    data class Game(val league: String, val id: String, val start: String, val state: String,
                    val detail: String, val home: Side, val away: Side)
    data class SportsConfig(val leagues: List<String>, val follow: List<String>,
                            val allLive: List<String>, val leagueNames: Map<String, String>)

    val liveConfigured get() = Live.configured

    // ---------------- mapping ----------------
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

    private fun railItem(o: JSONObject) = RailItem(
        id = o.optString("id"), kind = o.optString("kind"), name = o.optString("name"),
        year = o.optString("year"), logo = o.optString("logo"),
        rating = o.optDouble("rating", 0.0), url = o.optString("url"))

    suspend fun home(): HomeData = withContext(Dispatchers.IO) {
        val j = Live.home()
        HomeData(
            featured = (j.optJSONArray("featured") ?: JSONArray()).objects().map { o ->
                Featured(o.optString("id"), o.optString("name"), o.optString("year"), o.optString("backdrop"),
                    o.optString("poster"), o.optString("plot"), o.optString("genre"), o.optDouble("rating", 0.0), o.optString("url"))
            },
            movies = (j.optJSONArray("movies") ?: JSONArray()).objects().map(::railItem),
            series = (j.optJSONArray("series") ?: JSONArray()).objects().map { railItem(it).copy(kind = "series") },
            providers = (j.optJSONArray("providers") ?: JSONArray()).objects().map { o ->
                Provider(o.optString("id"), o.optString("name"),
                    o.optString("vod").takeIf { it.isNotEmpty() && it != "null" },
                    o.optString("series").takeIf { it.isNotEmpty() && it != "null" })
            })
    }

    suspend fun categories(kind: String): List<Category> = withContext(Dispatchers.IO) {
        Live.categories(kind).objects().map { Category(it.optString("id"), it.optString("name")) }
    }

    /** One category's entries. Titles come back cleaned; live channels keep their names whole. */
    suspend fun items(kind: String, cat: String): List<RailItem> = withContext(Dispatchers.IO) {
        val arr = if (kind == "series") Live.seriesList(cat) else Live.channels(kind, cat)
        arr.objects().map { o ->
            val raw = o.optString("name")
            val (title, year) = if (kind == "live") raw to "" else Live.cleanTitle(raw)
            RailItem(o.optString("id"), kind, title, year, o.optString("logo"),
                o.optDouble("rating", 0.0), o.optString("url"))
        }
    }

    suspend fun search(q: String): List<RailItem> = withContext(Dispatchers.IO) {
        Live.search(q).objects().map(::railItem)
    }

    suspend fun seasons(seriesId: String): Pair<String, List<Season>> = withContext(Dispatchers.IO) {
        val j = Live.episodes(seriesId)
        j.optString("name") to (j.optJSONArray("seasons") ?: JSONArray()).objects().map { se ->
            Season(se.optInt("season"), (se.optJSONArray("episodes") ?: JSONArray()).objects().map { e ->
                Episode(e.optString("id"), e.optInt("num"), e.optString("title"), e.optString("url"))
            })
        }
    }

    // ---------------- sports ----------------
    fun sportsConfig(): SportsConfig {
        val st = (Store.readJson("home-settings.json") as? JSONObject)?.optJSONObject("sports") ?: JSONObject()
        fun list(k: String) = (st.optJSONArray(k) ?: JSONArray()).let { a -> (0 until a.length()).map { a.optString(it) } }
        return SportsConfig(list("leagues"), list("follow"), list("all_live"),
            list("leagues").associateWith { Scores.leagueName(it) })
    }

    suspend fun games(leagues: List<String>): List<Game> = withContext(Dispatchers.IO) {
        if (leagues.isEmpty()) return@withContext emptyList()
        (Scores.get(leagues).optJSONArray("games") ?: JSONArray()).objects().map { g ->
            fun side(k: String) = (g.optJSONObject(k) ?: JSONObject()).let {
                Side(it.optString("abbr"), it.optString("name"), it.optString("score"), it.optString("logo"))
            }
            Game(g.optString("league"), g.optString("id"), g.optString("start"), g.optString("state"),
                g.optString("detail"), side("home"), side("away"))
        }
    }
}

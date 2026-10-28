package dev.couchtv.ontv

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live TV, films and series from the household's own line: an Xtream-Codes playlist (server,
 * username, password), the format most IPTV subscriptions hand out. Set over /api/settings
 * {"live": {server, username, password}}; the credentials stay in this app's storage and the
 * pages only ever see ready stream links. The full catalogues run to tens of thousands of
 * entries, so everything is asked for per category and cached.
 */
object Live {
    private val cache = HashMap<String, Pair<Long, String>>()   // url -> fetched-at to body

    fun cfg(): JSONObject? = Store.str("live_cfg")?.let { runCatching { JSONObject(it) }.getOrNull() }
        ?.takeIf { it.optString("server").isNotEmpty() && it.optString("username").isNotEmpty() }

    val configured get() = cfg() != null

    fun save(o: JSONObject?) {
        if (o == null || o.optString("server").isEmpty()) { Store.putStr("live_cfg", null); clear(); return }
        Store.putStr("live_cfg", JSONObject()
            .put("server", o.optString("server").trim().removeSuffix("/"))
            .put("username", o.optString("username").trim())
            .put("password", o.optString("password").trim()).toString())
        clear()
    }

    @Synchronized fun clear() { cache.clear() }

    private fun fetch(url: String, ttl: Long): String {
        synchronized(this) { cache[url]?.let { (at, body) -> if (System.currentTimeMillis() - at < ttl) return body } }
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 30_000
        val body = try { c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
        if (body.isEmpty()) throw ApiError(502, "The playlist server sent nothing back.")
        synchronized(this) { cache[url] = System.currentTimeMillis() to body }
        return body
    }

    private fun apiUrl(action: String, extra: String = ""): String {
        val c = cfg() ?: throw ApiError(409, "No playlist on this TV yet. Add your line's server, username and password in settings.")
        return "${c.getString("server")}/player_api.php?username=${c.getString("username")}&password=${c.getString("password")}&action=$action$extra"
    }

    /** kind: live | vod | series. */
    fun categories(kind: String): JSONArray {
        val action = when (kind) {
            "live" -> "get_live_categories"; "vod" -> "get_vod_categories"; "series" -> "get_series_categories"
            else -> throw ApiError(400, "kind must be live, vod or series")
        }
        val arr = JSONArray(fetch(apiUrl(action), 6 * 3600_000))
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            out.put(JSONObject().put("id", c.optString("category_id")).put("name", c.optString("category_name")))
        }
        return out
    }

    /** One category's entries, capped, with ready stream links. kind: live | vod. */
    fun channels(kind: String, cat: String): JSONArray {
        val c = cfg() ?: throw ApiError(409, "No playlist on this TV yet.")
        val action = when (kind) { "live" -> "get_live_streams"; "vod" -> "get_vod_streams"; else -> throw ApiError(400, "kind must be live or vod") }
        val arr = JSONArray(fetch(apiUrl(action, "&category_id=$cat"), 3600_000))
        val base = c.getString("server"); val u = c.getString("username"); val p = c.getString("password")
        val out = JSONArray()
        for (i in 0 until minOf(arr.length(), 500)) {
            val s = arr.getJSONObject(i)
            val id = s.optString("stream_id")
            val url = if (kind == "live") "$base/live/$u/$p/$id.m3u8"
            else "$base/movie/$u/$p/$id.${s.optString("container_extension").ifEmpty { "mp4" }}"
            out.put(JSONObject().put("id", id).put("name", s.optString("name"))
                .put("logo", s.optString("stream_icon")).put("url", url)
                .put("rating", s.optDouble("rating_5based", 0.0).takeIf { it > 0 } ?: JSONObject.NULL))
        }
        return out
    }

    /** The series in one category: covers and ratings, no links (episodes() has those). */
    fun seriesList(cat: String): JSONArray {
        val arr = JSONArray(fetch(apiUrl("get_series", "&category_id=$cat"), 3600_000))
        val out = JSONArray()
        for (i in 0 until minOf(arr.length(), 500)) {
            val s = arr.getJSONObject(i)
            out.put(JSONObject().put("id", s.optString("series_id")).put("name", s.optString("name"))
                .put("logo", s.optString("cover"))
                .put("rating", s.optDouble("rating_5based", 0.0).takeIf { it > 0 } ?: JSONObject.NULL))
        }
        return out
    }

    // ---------------------------------------------------------------- the home rails

    // "EN - The Weight (2026)" and "4K-OSN+ - Dune (2021) (US)" become a title and a year.
    private val PREFIX = Regex("^(?:4K-|8K-)?[A-Z]{2}[A-Za-z+]*\\s*-\\s*")
    private val YEAR = Regex("\\s*\\((\\d{4})\\)\\s*(?:\\([A-Z]{2}\\))?\\s*$")

    fun cleanTitle(name: String): Pair<String, String> {
        var t = name.replace(PREFIX, "")
        var year = ""
        YEAR.find(t)?.let { year = it.groupValues[1]; t = t.substring(0, it.range.first) }
        return t.trim() to year
    }

    private fun firstCat(kind: String, vararg wanted: String): JSONObject? {
        val cats = categories(kind)
        for (w in wanted) for (i in 0 until cats.length()) {
            val c = cats.getJSONObject(i)
            if (c.optString("name").uppercase().contains(w)) return c
        }
        return if (cats.length() > 0) cats.getJSONObject(0) else null
    }

    /** One film's detail (plot, genre, backdrop), straight off the line. */
    private fun vodInfo(id: String): JSONObject =
        JSONObject(fetch(apiUrl("get_vod_info", "&vod_id=$id"), 24 * 3600_000)).optJSONObject("info") ?: JSONObject()

    private fun railItem(s: JSONObject, kind: String): JSONObject {
        val (title, year) = cleanTitle(s.optString("name"))
        return JSONObject().put("id", s.optString(if (kind == "series") "series_id" else "stream_id"))
            .put("kind", kind).put("name", title).put("year", year)
            .put("logo", s.optString(if (kind == "series") "cover" else "stream_icon"))
            .put("rating", s.optDouble("rating_5based", 0.0).takeIf { it > 0 } ?: JSONObject.NULL)
    }

    /**
     * Everything the home page's hero and rails need, in one call: the newest films with their
     * backdrops and plots, a trending series row, and the branded groups the line carries.
     */
    @Synchronized fun home(): JSONObject {
        homeCache?.let { (at, j) -> if (System.currentTimeMillis() - at < 6 * 3600_000) return JSONObject(j.toString()) }
        val c = cfg() ?: throw ApiError(409, "No playlist on this TV yet.")
        val base = c.getString("server"); val u = c.getString("username"); val p = c.getString("password")

        val newCat = firstCat("vod", "NEW RELEASE", "TRENDING", "NEW") ?: throw ApiError(502, "The line lists no film groups.")
        val rows = JSONArray(fetch(apiUrl("get_vod_streams", "&category_id=${newCat.getString("id")}"), 3600_000))
        val byAdded = (0 until rows.length()).map { rows.getJSONObject(it) }
            .sortedByDescending { it.optString("added").toLongOrNull() ?: 0 }

        val featured = JSONArray()
        for (s in byAdded.take(7)) {
            val id = s.optString("stream_id")
            val info = runCatching { vodInfo(id) }.getOrDefault(JSONObject())
            val backdrop = info.optJSONArray("backdrop_path")?.optString(0)?.ifEmpty { null }
            val (title, year) = cleanTitle(s.optString("name"))
            featured.put(JSONObject().put("id", id).put("kind", "vod").put("name", title).put("year", year)
                .put("poster", s.optString("stream_icon")).put("backdrop", backdrop ?: s.optString("stream_icon"))
                .put("plot", info.optString("plot").ifEmpty { info.optString("description") })
                .put("genre", info.optString("genre")).put("rating", s.optDouble("rating_5based", 0.0).takeIf { it > 0 } ?: JSONObject.NULL)
                .put("url", "$base/movie/$u/$p/$id.${s.optString("container_extension").ifEmpty { "mp4" }}"))
        }

        val movies = JSONArray().also { arr -> byAdded.drop(7).take(15).forEach { s ->
            arr.put(railItem(s, "vod").put("url", "$base/movie/$u/$p/${s.optString("stream_id")}.${s.optString("container_extension").ifEmpty { "mp4" }}")) } }

        val seriesCat = firstCat("series", "NETFLIX SERIES", "TRENDING", "OSN")
        val series = JSONArray()
        seriesCat?.let {
            val sr = JSONArray(fetch(apiUrl("get_series", "&category_id=${it.getString("id")}"), 3600_000))
            val bySeen = (0 until sr.length()).map { i -> sr.getJSONObject(i) }
                .sortedByDescending { r -> r.optString("last_modified").toLongOrNull() ?: 0 }
            bySeen.take(15).forEach { r -> series.put(railItem(r, "series")) }
        }

        val out = JSONObject().put("ok", true).put("featured", featured).put("movies", movies)
            .put("series", series).put("providers", providers())
        homeCache = System.currentTimeMillis() to out
        return JSONObject(out.toString())
    }
    private var homeCache: Pair<Long, JSONObject>? = null

    /** The branded groups the line carries, for the Browse by Provider row. */
    private fun providers(): JSONArray {
        val brands = listOf("NETFLIX" to "Netflix", "OSN" to "OSN+", "DISNEY" to "Disney+", "APPLE" to "Apple TV+",
            "PRIME" to "Prime Video", "HBO" to "HBO Max", "PARAMOUNT" to "Paramount+", "CRUNCHYROLL" to "Crunchyroll",
            "DISCOVERY" to "discovery+", "HULU" to "Hulu", "PEACOCK" to "Peacock", "STARZ" to "Starz")
        val out = JSONArray()
        val vodCats = runCatching { categories("vod") }.getOrDefault(JSONArray())
        val serCats = runCatching { categories("series") }.getOrDefault(JSONArray())
        fun find(cats: JSONArray, key: String): String? {
            for (i in 0 until cats.length()) {
                val c = cats.getJSONObject(i)
                if (c.optString("name").uppercase().contains(key)) return c.optString("id")
            }
            return null
        }
        for ((key, name) in brands) {
            val vod = find(vodCats, key); val ser = find(serCats, key)
            if (vod != null || ser != null) out.put(JSONObject().put("id", key.lowercase()).put("name", name)
                .put("vod", vod ?: JSONObject.NULL).put("series", ser ?: JSONObject.NULL))
        }
        return out
    }

    // ---------------------------------------------------------------- search

    private data class Row(val kind: String, val id: String, val name: String, val logo: String, val ext: String, val rating: Double)
    private var index: Pair<Long, List<Row>>? = null

    /** A lean index over the whole line (tens of thousands of names), built once, kept six hours. */
    @Synchronized private fun searchIndex(): List<Row> {
        index?.let { (at, rows) -> if (System.currentTimeMillis() - at < 6 * 3600_000) return rows }
        val rows = ArrayList<Row>(60_000)
        fun pull(kind: String, action: String, idKey: String, logoKey: String) {
            val c = URL(apiUrl(action)).openConnection() as HttpURLConnection
            c.connectTimeout = 8000; c.readTimeout = 60_000
            val body = try { c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
            val arr = JSONArray(body)   // parsed once and reduced at once; the body is never cached
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                rows.add(Row(kind, s.optString(idKey), s.optString("name"), s.optString(logoKey),
                    s.optString("container_extension"), s.optDouble("rating_5based", 0.0)))
            }
        }
        runCatching { pull("live", "get_live_streams", "stream_id", "stream_icon") }
        runCatching { pull("vod", "get_vod_streams", "stream_id", "stream_icon") }
        runCatching { pull("series", "get_series", "series_id", "cover") }
        if (rows.isEmpty()) throw ApiError(502, "The line's lists would not load for search.")
        index = System.currentTimeMillis() to rows
        return rows
    }

    /** Name search across live, films and series; films and series first, best prefix matches on top. */
    fun search(qIn: String): JSONArray {
        val c = cfg() ?: throw ApiError(409, "No playlist on this TV yet.")
        val base = c.getString("server"); val u = c.getString("username"); val p = c.getString("password")
        val q = qIn.trim().lowercase()
        if (q.length < 2) return JSONArray()
        val kindRank = mapOf("vod" to 0, "series" to 1, "live" to 2)
        val hits = searchIndex().asSequence().filter { it.name.lowercase().contains(q) }.take(400).toList()
            .sortedWith(compareBy({ kindRank[it.kind] ?: 3 }, { !it.name.lowercase().startsWith(q) && !cleanTitle(it.name).first.lowercase().startsWith(q) }))
            .take(48)
        val out = JSONArray()
        for (r in hits) {
            val (title, year) = cleanTitle(r.name)
            val o = JSONObject().put("kind", r.kind).put("id", r.id).put("name", title).put("year", year)
                .put("logo", r.logo).put("rating", r.rating.takeIf { it > 0 } ?: JSONObject.NULL)
            when (r.kind) {
                "live" -> o.put("url", "$base/live/$u/$p/${r.id}.m3u8")
                "vod" -> o.put("url", "$base/movie/$u/$p/${r.id}.${r.ext.ifEmpty { "mp4" }}")
            }
            out.put(o)
        }
        return out
    }

    /** One series: its seasons, each with playable episodes. */
    fun episodes(seriesId: String): JSONObject {
        val c = cfg() ?: throw ApiError(409, "No playlist on this TV yet.")
        val j = JSONObject(fetch(apiUrl("get_series_info", "&series_id=$seriesId"), 3600_000))
        val base = c.getString("server"); val u = c.getString("username"); val p = c.getString("password")
        val eps = j.optJSONObject("episodes") ?: JSONObject()
        val seasons = JSONArray()
        for (season in eps.keys().asSequence().sortedBy { it.toIntOrNull() ?: 0 }.toList()) {
            val list = eps.optJSONArray(season) ?: continue
            val out = JSONArray()
            for (i in 0 until list.length()) {
                val e = list.getJSONObject(i)
                out.put(JSONObject().put("id", e.optString("id")).put("num", e.optInt("episode_num", i + 1))
                    .put("title", e.optString("title"))
                    .put("url", "$base/series/$u/$p/${e.optString("id")}.${e.optString("container_extension").ifEmpty { "mp4" }}"))
            }
            seasons.put(JSONObject().put("season", season.toIntOrNull() ?: 0).put("episodes", out))
        }
        return JSONObject().put("name", j.optJSONObject("info")?.optString("name") ?: "").put("seasons", seasons)
    }
}

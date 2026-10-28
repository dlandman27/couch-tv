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

package dev.couchtv.ontv

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live scores for the home screen's sports strip and hub. ESPN's public scoreboards: free, no
 * account, no key. Cached per league: under a minute while a game is on, five minutes otherwise.
 */
object Scores {
    private val PATHS = mapOf(
        "nfl" to "football/nfl", "mlb" to "baseball/mlb", "nhl" to "hockey/nhl", "nba" to "basketball/nba",
        "ncaaf" to "football/college-football", "mls" to "soccer/usa.1",
        "eng.1" to "soccer/eng.1", "uefa.champions" to "soccer/uefa.champions",
    )
    private val NAMES = mapOf(
        "nfl" to "NFL", "mlb" to "MLB", "nhl" to "NHL", "nba" to "NBA", "ncaaf" to "College football",
        "mls" to "MLS", "eng.1" to "Premier League", "uefa.champions" to "Champions League",
    )
    private val cache = HashMap<String, Pair<Long, JSONArray>>()

    fun leagueName(id: String) = NAMES[id] ?: id

    /** Games for these league ids, oldest cache entry refreshed first. Unknown ids are skipped. */
    @Synchronized fun get(leagues: List<String>): JSONObject {
        val games = JSONArray()
        val lgs = JSONArray()
        for (lg in leagues) {
            val path = PATHS[lg] ?: continue
            lgs.put(JSONObject().put("id", lg).put("name", leagueName(lg)))
            for (g in fetch(lg, path)) games.put(g)
        }
        return JSONObject().put("ok", true).put("leagues", lgs).put("games", games)
    }

    private fun fetch(lg: String, path: String): List<JSONObject> {
        val now = System.currentTimeMillis()
        cache[lg]?.let { (at, arr) ->
            val live = (0 until arr.length()).any { arr.getJSONObject(it).optString("state") == "in" }
            if (now - at < if (live) 45_000 else 300_000) return list(arr)
        }
        try {
            val c = URL("https://site.api.espn.com/apis/site/v2/sports/$path/scoreboard").openConnection() as HttpURLConnection
            c.connectTimeout = 6000; c.readTimeout = 8000
            val j = JSONObject(c.inputStream.bufferedReader().readText()); c.disconnect()
            val events = j.optJSONArray("events") ?: JSONArray()
            val out = JSONArray()
            for (i in 0 until events.length()) parse(lg, events.getJSONObject(i))?.let { out.put(it) }
            cache[lg] = now to out
        } catch (e: Exception) { android.util.Log.w("couchtv", "scores $lg: ${e.message}") }
        return cache[lg]?.let { list(it.second) } ?: emptyList()
    }

    private fun list(arr: JSONArray) = (0 until arr.length()).map { arr.getJSONObject(it) }

    private fun parse(lg: String, e: JSONObject): JSONObject? {
        val comp = e.optJSONArray("competitions")?.optJSONObject(0) ?: return null
        val sides = comp.optJSONArray("competitors") ?: return null
        val status = e.optJSONObject("status")?.optJSONObject("type") ?: return null
        var home: JSONObject? = null; var away: JSONObject? = null
        for (i in 0 until sides.length()) {
            val s = sides.getJSONObject(i)
            val team = s.optJSONObject("team") ?: continue
            val o = JSONObject().put("abbr", team.optString("abbreviation"))
                .put("name", team.optString("shortDisplayName", team.optString("displayName")))
                .put("score", s.optString("score")).put("logo", team.optString("logo"))
            if (s.optString("homeAway") == "home") home = o else away = o
        }
        if (home == null || away == null) return null
        return JSONObject().put("league", lg).put("id", e.optString("id")).put("start", e.optString("date"))
            .put("state", status.optString("state"))                  // pre | in | post
            .put("detail", status.optString("shortDetail"))           // "Final", "8:00 PM EDT", "6:29 - 1st"
            .put("home", home).put("away", away)
    }
}

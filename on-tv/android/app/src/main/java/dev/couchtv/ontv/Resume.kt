package dev.couchtv.ontv

import org.json.JSONObject

/**
 * How far into each film the viewer got, so Home's Continue Watching can show a progress bar and
 * "N min left", and the player can pick up where they stopped. Keyed by the stream link, kept in
 * the app's own store. Live channels have no duration, so they are never marked.
 */
object Resume {
    private const val FILE = "resume.json"
    private const val CAP = 60

    data class Mark(val pos: Long, val dur: Long)

    @Volatile private var cache: JSONObject? = null
    private fun data(): JSONObject = cache ?: synchronized(this) {
        cache ?: ((Store.readJson(FILE) as? JSONObject) ?: JSONObject()).also { cache = it }
    }

    fun get(url: String): Mark? {
        if (url.isEmpty()) return null
        val v = data().optString(url).takeIf { it.isNotEmpty() } ?: return null
        val parts = v.split(","); if (parts.size != 2) return null
        val pos = parts[0].toLongOrNull() ?: return null
        val dur = parts[1].toLongOrNull() ?: return null
        return Mark(pos, dur)
    }

    /** Record a stop. Finishing (near the end) clears the mark so it leaves Continue Watching. */
    @Synchronized fun save(url: String, pos: Long, dur: Long) {
        if (url.isEmpty() || dur <= 0) return
        val o = data()
        if (pos <= 15_000 || pos >= dur * 0.97) { o.remove(url) }
        else {
            o.put(url, "$pos,$dur")
            while (o.length() > CAP) o.remove(o.keys().next())   // oldest-ish; the map is small
        }
        cache = o
        runCatching { Store.writeJson(FILE, o) }
    }

    /** 0f..1f watched, or null when there is nothing meaningful to show. */
    fun progress(url: String): Float? = get(url)?.let { if (it.dur > 0) (it.pos.toFloat() / it.dur).coerceIn(0f, 1f) else null }

    fun remaining(url: String): String? = get(url)?.let {
        if (it.dur <= 0 || it.pos >= it.dur * 0.97) return null
        val mins = ((it.dur - it.pos) / 60_000).toInt()
        if (mins <= 0) "A few min left" else "$mins min left"
    }
}

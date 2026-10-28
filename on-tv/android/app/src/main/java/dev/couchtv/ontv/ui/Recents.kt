package dev.couchtv.ontv.ui

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import dev.couchtv.ontv.App
import org.json.JSONArray
import org.json.JSONObject

/**
 * The last handful of films and shows the viewer opened to watch, kept on the TV so Home can lead
 * with "Continue Watching". No account and no server: a small JSON list in the app's own prefs,
 * newest first, de-duped, capped. A Compose state list, so Home redraws the moment a new play lands.
 * Each one keeps a landscape backdrop as well as its poster, for the wide Continue Watching cards.
 */
object Recents {
    private const val CAP = 18
    private val prefs by lazy { App.instance.getSharedPreferences("recents", Context.MODE_PRIVATE) }

    data class Rec(val id: String, val kind: String, val name: String, val poster: String,
                   val backdrop: String, val url: String, val rating: Double)

    /** What Home renders. Loaded once from prefs, kept in sync as plays come in. */
    val items = mutableStateListOf<Rec>()

    init { load() }

    private fun load() = runCatching {
        val arr = JSONArray(prefs.getString("list", "[]"))
        items.clear()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            items.add(Rec(o.optString("id"), o.optString("kind"), o.optString("name"), o.optString("poster"),
                o.optString("backdrop"), o.optString("url"), o.optDouble("rating", 0.0)))
        }
    }

    /** Record that this title was opened to watch; moves it to the front of the list. */
    fun note(id: String, kind: String, name: String, poster: String, backdrop: String, url: String, rating: Double = 0.0) {
        if (id.isEmpty() && url.isEmpty()) return
        items.removeAll { if (id.isNotEmpty()) it.id == id else it.url == url }
        items.add(0, Rec(id, kind, name, poster, backdrop.ifEmpty { poster }, url, rating))
        while (items.size > CAP) items.removeAt(items.size - 1)
        save()
    }

    private fun save() = runCatching {
        val arr = JSONArray()
        items.forEach { r ->
            arr.put(JSONObject().put("id", r.id).put("kind", r.kind).put("name", r.name)
                .put("poster", r.poster).put("backdrop", r.backdrop).put("url", r.url).put("rating", r.rating))
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}

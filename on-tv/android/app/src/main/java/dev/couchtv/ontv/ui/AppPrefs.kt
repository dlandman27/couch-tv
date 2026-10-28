package dev.couchtv.ontv.ui

import androidx.compose.runtime.mutableStateListOf
import dev.couchtv.ontv.Store
import org.json.JSONArray
import org.json.JSONObject

/**
 * How the viewer has arranged their apps on this TV: a custom order, the ones pinned to the Home
 * row, and the ones hidden. Kept in the app's own store and exposed as Compose state, so the Apps
 * screen and Home's "Your Apps" row both react the moment something changes. Separate from the
 * phone remote's app-order so the two never fight.
 */
object AppPrefs {
    private const val FILE = "native-apps.json"

    val order = mutableStateListOf<String>()    // full package order the viewer has set
    val pinned = mutableStateListOf<String>()   // shown first on Home's Your Apps
    val hidden = mutableStateListOf<String>()

    init { load() }

    private fun load() = runCatching {
        val o = Store.readJson(FILE) as? JSONObject ?: return@runCatching
        fun fill(list: MutableList<String>, key: String) {
            val arr = o.optJSONArray(key) ?: JSONArray()
            list.clear(); for (i in 0 until arr.length()) list.add(arr.optString(i))
        }
        fill(order, "order"); fill(pinned, "pinned"); fill(hidden, "hidden")
    }

    private fun save() = runCatching {
        Store.writeJson(FILE, JSONObject()
            .put("order", JSONArray(order)).put("pinned", JSONArray(pinned)).put("hidden", JSONArray(hidden)))
    }

    /** All packages in the viewer's order: known order first, anything new appended in its given order. */
    fun applyOrder(all: List<String>): List<String> {
        val rank = order.withIndex().associate { (i, p) -> p to i }
        return all.sortedWith(compareBy({ rank[it] ?: Int.MAX_VALUE }, { all.indexOf(it) }))
    }

    fun isPinned(pkg: String) = pkg in pinned
    fun isHidden(pkg: String) = pkg in hidden

    fun togglePinned(pkg: String) { if (!pinned.remove(pkg)) pinned.add(pkg); save() }
    fun toggleHidden(pkg: String) { if (!hidden.remove(pkg)) hidden.add(pkg); save() }

    /** Swap two packages in the order (two-tap reorder on the TV). `all` seeds the order on first use. */
    fun swap(all: List<String>, a: String, b: String) {
        val list = applyOrder(all).toMutableList()
        val ia = list.indexOf(a); val ib = list.indexOf(b)
        if (ia < 0 || ib < 0) return
        list[ia] = b; list[ib] = a
        order.clear(); order.addAll(list); save()
    }

    /** Put back an order saved before a move started (Back cancels a reorder). */
    fun restore(saved: List<String>) { order.clear(); order.addAll(saved); save() }

    /** Slide [pkg] [delta] places within the list the viewer can see ([visible]: Home's row or the
     * Apps grid), by swapping it with whatever sits there. No-op at either end. */
    fun moveBy(all: List<String>, visible: List<String>, pkg: String, delta: Int) {
        val i = visible.indexOf(pkg); val j = i + delta
        if (i < 0 || j !in visible.indices) return
        swap(all, pkg, visible[j])
    }
}

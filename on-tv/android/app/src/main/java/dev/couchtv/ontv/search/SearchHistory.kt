package dev.couchtv.ontv.search

import dev.couchtv.ontv.Store
import org.json.JSONArray

/** The last few searches the viewer committed (Search key, a result opened, or voice), newest first. */
object SearchHistory {
    private const val KEY = "search_recent"
    private const val CAP = 5

    fun recent(): List<String> = runCatching {
        val a = JSONArray(Store.str(KEY) ?: "[]"); (0 until a.length()).map { a.optString(it) }
    }.getOrDefault(emptyList())

    fun add(q: String) {
        val t = q.trim(); if (t.length < 2) return
        val list = (listOf(t) + recent().filter { !it.equals(t, ignoreCase = true) }).take(CAP)
        Store.putStr(KEY, JSONArray(list).toString())
    }
}

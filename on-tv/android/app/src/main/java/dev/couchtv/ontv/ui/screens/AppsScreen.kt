package dev.couchtv.ontv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.Tv
import dev.couchtv.ontv.ui.AppPrefs
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.components.AppIcon
import dev.couchtv.ontv.ui.components.FilterChip
import dev.couchtv.ontv.ui.moveKeys
import androidx.compose.ui.draw.alpha

private enum class Mode { None, Reorder, Pin, Hide }

/**
 * Apps as a centered grid of square icons, Apple-TV style. Modes let the viewer reorder (tap two to
 * swap), pin an app to Home, or hide one. Square launcher icons come straight from the package manager.
 */
@Composable
fun AppsScreen() {
    val ctx = LocalContext.current
    val all = remember {
        val pm = ctx.packageManager
        Tv.tvLaunchables().map { ri ->
            val icon = ri.activityInfo.loadIcon(pm) ?: ri.loadIcon(pm)
            Triple(ri.activityInfo.packageName, ri.loadLabel(pm).toString(), icon)
        }.filter { it.first != ctx.packageName }
    }
    var mode by remember { mutableStateOf(Mode.None) }
    var pickup by remember { mutableStateOf<String?>(null) }
    var before by remember { mutableStateOf<List<String>>(emptyList()) }   // order to restore on Back

    val orderedPkgs = AppPrefs.applyOrder(all.map { it.first })
    val byPkg = all.associateBy { it.first }
    val ordered = orderedPkgs.mapNotNull { byPkg[it] }
    val visible = if (mode == Mode.Hide) ordered else ordered.filter { !AppPrefs.isHidden(it.first) }

    fun onApp(pkg: String) {
        when (mode) {
            Mode.None -> runCatching { Tv.launchPackage(pkg) }
            Mode.Pin -> AppPrefs.togglePinned(pkg)
            Mode.Hide -> AppPrefs.toggleHidden(pkg)
            Mode.Reorder -> pickup = if (pickup == pkg) null else pkg   // OK lifts an app, OK again sets it down
        }
    }
    fun pick(m: Mode) { mode = if (mode == m) Mode.None else m; pickup = null }

    val hint = when {
        pickup != null -> "Arrows to move  ·  OK to place  ·  Back to cancel"
        mode == Mode.Reorder -> "Press OK on an app to pick it up. (Holding OK on any app does this too.)"
        mode == Mode.Pin -> "Tap apps to pin them to the Home row."
        mode == Mode.Hide -> "Tap to hide; tap a dimmed app to bring it back."
        else -> null
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 56.dp, end = 56.dp, top = 56.dp, bottom = 28.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header", span = { GridItemSpan(6) }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
                Text("Your Apps", style = Cj.RowTitle, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("⇄ Reorder", on = mode == Mode.Reorder, onOk = { pick(Mode.Reorder) })
                    FilterChip("Pin to Home", on = mode == Mode.Pin, onOk = { pick(Mode.Pin) })
                    FilterChip("Hide App", on = mode == Mode.Hide, onOk = { pick(Mode.Hide) })
                }
            }
        }
        // the hint line is always reserved, so the grid never jumps when instructions appear
        item(key = "hint", span = { GridItemSpan(6) }) {
            Box(Modifier.padding(bottom = 2.dp).size(width = 600.dp, height = 18.dp)) {
                if (hint != null) Text(hint, style = Cj.Small.copy(color = Cj.Dim))
            }
        }
        itemsIndexed(visible, key = { _, a -> a.first }) { _, (pkg, label, icon) ->
            val badge = when {
                mode == Mode.Pin -> if (AppPrefs.isPinned(pkg)) "📌" else "＋"
                mode == Mode.Hide -> if (AppPrefs.isHidden(pkg)) "＋" else "✕"
                mode == Mode.None && AppPrefs.isPinned(pkg) -> "📌"
                else -> null
            }
            AppIcon(label, icon,
                // while an app is held, OK places it whatever mode is on; Back puts the old order back
                onOk = { if (pickup == pkg) pickup = null else { if (mode == Mode.Reorder) before = AppPrefs.order.toList(); onApp(pkg) } },
                onLongClick = { if (mode != Mode.Hide) { before = AppPrefs.order.toList(); pickup = pkg } },
                modifier = Modifier
                    .moveKeys(pickup == pkg, cols = 6,
                        onMove = { d -> AppPrefs.moveBy(all.map { it.first }, visible.map { it.first }, pkg, d) },
                        onCancel = { AppPrefs.restore(before); pickup = null }),
                badge = badge, dim = AppPrefs.isHidden(pkg) && mode == Mode.Hide, held = pickup == pkg)
        }
        if (mode == Mode.None) item(key = "add") { AddIcon(onOk = {
            runCatching { Tv.launchPackage("com.amazon.venezia") }.onFailure { runCatching { Tv.launchPackage("com.android.vending") } }
        }) }
    }
}

@Composable
private fun AddIcon(onOk: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.width(128.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onOk,
            shape = ClickableSurfaceDefaults.shape(Cj.Card),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Cj.Surface2, focusedContainerColor = Cj.Hover),
            border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = Cj.Card)),
            modifier = Modifier.size(104.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("＋", style = Cj.HeroTitle.copy(fontSize = 34.sp, color = Cj.Dim))
            }
        }
        Text("Add app", style = Cj.Small.copy(color = Cj.Dim), modifier = Modifier.padding(top = 8.dp))
    }
}

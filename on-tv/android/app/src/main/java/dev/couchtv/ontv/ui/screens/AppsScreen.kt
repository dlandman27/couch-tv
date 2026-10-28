package dev.couchtv.ontv.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.couchtv.ontv.Tv
import dev.couchtv.ontv.ui.components.WideCard

/** Apps: every TV app as its banner, straight from the package manager, launched natively. */
@Composable
fun AppsScreen() {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val apps = remember {
        Tv.tvLaunchables().map { ri ->
            val label = ri.loadLabel(pm).toString()
            val banner = ri.activityInfo.loadBanner(pm) ?: ri.activityInfo.applicationInfo.loadBanner(pm)
                ?: ri.loadIcon(pm)
            Triple(ri.activityInfo.packageName, label, banner)
        }.filter { it.first != ctx.packageName }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(horizontal = 44.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(apps, key = { _, a -> a.first }) { _, (pkg, label, banner) ->
            WideCard(name = label, image = banner, width = 210.dp,
                onOk = { runCatching { Tv.launchPackage(pkg) } })
        }
    }
}

package dev.couchtv.ontv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo
import dev.couchtv.ontv.ui.components.ChannelRow
import dev.couchtv.ontv.ui.components.ChipRowSkeleton
import dev.couchtv.ontv.ui.components.FilterChip
import dev.couchtv.ontv.ui.components.PosterCard
import dev.couchtv.ontv.ui.components.PosterGridSkeleton

/**
 * Movies and Shows as one wall: every title together, narrowed by streaming service (the chips),
 * drilled into by category (the reveal), and ordered by the sort. Live TV keeps a channel grid,
 * since there the category IS the right axis. The page mounts at once with skeletons so a tab
 * switch never blocks. kind is vod, series or live.
 */
@Composable
fun BrowseScreen(
    kind: String,
    startCat: String?,
    onPlay: (String) -> Unit,
    onOpen: (UiRepo.RailItem) -> Unit
) {
    if (kind == "live") LiveBrowse(startCat, onPlay)
    else CatalogBrowse(kind, startCat, onOpen)
}

/** Movies / Shows: one unified poster wall with service + category + sort filters. */
@Composable
private fun CatalogBrowse(kind: String, startCat: String?, onOpen: (UiRepo.RailItem) -> Unit) {
    var services by remember(kind) { mutableStateOf<List<UiRepo.Provider>>(emptyList()) }
    var cats by remember(kind) { mutableStateOf<List<UiRepo.Category>>(emptyList()) }
    // a provider tile on Home hands off a category id; otherwise we start on "All".
    var selService by rememberSaveable(kind) { mutableStateOf<String?>(null) }
    var selCat by rememberSaveable(kind, startCat) { mutableStateOf(startCat) }
    var showCats by rememberSaveable(kind) { mutableStateOf(false) }
    var sort by rememberSaveable(kind) { mutableStateOf(0) }   // 0 trending, 1 A–Z, 2 year
    var items by remember(kind) { mutableStateOf<List<UiRepo.RailItem>>(emptyList()) }
    var loading by remember(kind) { mutableStateOf(true) }
    var error by remember(kind) { mutableStateOf<String?>(null) }

    LaunchedEffect(kind) {
        runCatching { services = UiRepo.services(); cats = UiRepo.categories(kind) }
    }

    // which category id the wall loads: an explicit category wins, else the chosen service's, else all
    val fetchCat = selCat ?: selService?.let { id ->
        services.firstOrNull { it.id == id }?.let { if (kind == "series") it.seriesCat else it.vodCat }
    }
    LaunchedEffect(kind, fetchCat) {
        items = emptyList(); loading = true; error = null
        runCatching { items = UiRepo.catalog(kind, fetchCat ?: "") }
            .onFailure { error = it.message ?: "The line did not answer." }
        loading = false
    }

    val shown = remember(items, sort) {
        when (sort) {
            1 -> items.sortedBy { it.name.lowercase() }
            2 -> items.sortedByDescending { it.year.toIntOrNull() ?: 0 }
            else -> items
        }
    }
    val sortLabel = when (sort) { 1 -> "A–Z"; 2 -> "Year"; else -> "Trending" }

    Column(Modifier.fillMaxSize().padding(start = 44.dp, end = 44.dp, top = 54.dp)) {
        // title + count on the left, the sort and category controls on the right
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Text(if (kind == "series") "Shows" else "Movies", style = Cj.RowTitle)
            if (items.isNotEmpty())
                Text("  ${items.size} titles", style = Cj.Small.copy(color = Cj.Dim),
                    modifier = Modifier.padding(start = 10.dp))
            Box(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("↕  $sortLabel", on = false, onOk = { sort = (sort + 1) % 3 })
                FilterChip("Categories ▾", on = showCats, onOk = { showCats = !showCats })
            }
        }

        // service chips: All Services, then every brand the line carries
        if (services.isEmpty() && cats.isEmpty() && error == null) {
            ChipRowSkeleton(modifier = Modifier.padding(bottom = 14.dp))
        } else {
            LazyRow(
                state = rememberLazyListState(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 20.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                item(key = "all-svc") {
                    FilterChip("All Services", on = selService == null && selCat == null,
                        onOk = { selService = null; selCat = null })
                }
                itemsIndexed(services, key = { _, s -> "svc-" + s.id }) { _, s ->
                    FilterChip(s.name, on = selService == s.id,
                        onOk = { selService = s.id; selCat = null })
                }
            }
        }

        // the full category list, revealed on demand
        if (showCats && cats.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 20.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                item(key = "all-cat") {
                    FilterChip("All Genres", on = selCat == null, onOk = { selCat = null; selService = null })
                }
                itemsIndexed(cats, key = { _, c -> "cat-" + c.id }) { _, c ->
                    FilterChip(c.name, on = selCat == c.id, onOk = { selCat = c.id; selService = null })
                }
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                error != null && items.isEmpty() ->
                    Text(error!!, style = Cj.Body, modifier = Modifier.align(Alignment.Center))
                loading ->
                    PosterGridSkeleton(columns = 7, count = 21)
                shown.isEmpty() ->
                    Text("Nothing here yet.", style = Cj.Body, modifier = Modifier.align(Alignment.Center))
                else ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 30.dp)
                    ) {
                        itemsIndexed(shown, key = { _, c -> c.kind + c.id }) { _, c ->
                            PosterCard(c.name, c.logo, c.rating, width = null, onOk = { onOpen(c) })
                        }
                    }
            }
        }
    }
}

/** Live TV: channel groups across the top, a channel grid below. */
@Composable
private fun LiveBrowse(startCat: String?, onPlay: (String) -> Unit) {
    var cats by remember { mutableStateOf<List<UiRepo.Category>>(emptyList()) }
    var catId by rememberSaveable(startCat) { mutableStateOf(startCat) }
    var items by remember { mutableStateOf<List<UiRepo.RailItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            cats = UiRepo.categories("live")
            if (catId == null) catId = cats.firstOrNull()?.id
        }.onFailure { error = it.message ?: "The line did not answer." }
    }
    LaunchedEffect(catId) {
        val id = catId ?: return@LaunchedEffect
        items = emptyList(); loading = true; error = null
        runCatching { items = UiRepo.items("live", id) }
            .onFailure { error = it.message ?: "The line did not answer." }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(start = 44.dp, end = 44.dp, top = 54.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Text("Live TV", style = Cj.RowTitle)
        }
        if (cats.isEmpty() && error == null) {
            ChipRowSkeleton(modifier = Modifier.padding(bottom = 16.dp))
        } else {
            LazyRow(
                state = rememberLazyListState(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 20.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                itemsIndexed(cats, key = { _, c -> c.id }) { _, c ->
                    FilterChip(c.name, on = c.id == catId, onOk = { catId = c.id })
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                error != null && items.isEmpty() ->
                    Text(error!!, style = Cj.Body, modifier = Modifier.align(Alignment.Center))
                loading -> PosterGridSkeleton(columns = 3, count = 9)
                items.isEmpty() ->
                    Text("Nothing here yet.", style = Cj.Body, modifier = Modifier.align(Alignment.Center))
                else ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 30.dp)
                    ) {
                        itemsIndexed(items, key = { _, c -> c.id }) { _, c ->
                            ChannelRow(c.name, c.logo, onOk = { onPlay(c.url) })
                        }
                    }
            }
        }
    }
}

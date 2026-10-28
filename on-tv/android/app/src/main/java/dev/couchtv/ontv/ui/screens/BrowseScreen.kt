package dev.couchtv.ontv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
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
import dev.couchtv.ontv.ui.components.CategoryRail
import dev.couchtv.ontv.ui.components.ChannelRow
import dev.couchtv.ontv.ui.components.PosterCard

/**
 * Movies, Shows and Live TV: the category rail on the left, a virtualised poster wall (or channel
 * list) on the right. kind is vod, series or live; a provider tile can hand a category along.
 */
@Composable
fun BrowseScreen(
    kind: String,
    startCat: String?,
    onPlay: (String) -> Unit,
    onOpen: (UiRepo.RailItem) -> Unit
) {
    var cats by remember(kind) { mutableStateOf<List<UiRepo.Category>>(emptyList()) }
    var catId by rememberSaveable(kind, startCat) { mutableStateOf(startCat) }
    var items by remember(kind) { mutableStateOf<List<UiRepo.RailItem>>(emptyList()) }
    var note by remember(kind) { mutableStateOf("Loading…") }

    LaunchedEffect(kind) {
        runCatching {
            cats = UiRepo.categories(kind)
            if (catId == null) catId = cats.firstOrNull()?.id
        }.onFailure { note = it.message ?: "The line did not answer." }
    }
    LaunchedEffect(kind, catId) {
        val id = catId ?: return@LaunchedEffect
        items = emptyList(); note = "Loading…"
        runCatching { items = UiRepo.items(kind, id) }
            .onFailure { note = it.message ?: "The line did not answer." }
    }

    Row(Modifier.fillMaxSize().padding(start = 40.dp, top = 8.dp, end = 40.dp)) {
        CategoryRail(cats, catId, onPick = { catId = it.id }, modifier = Modifier.width(290.dp))
        Box(Modifier.weight(1f).padding(start = 22.dp)) {
            if (items.isEmpty()) {
                Text(note, style = Cj.Body, modifier = Modifier.align(Alignment.Center))
            } else if (kind == "live") {
                LazyColumn {
                    itemsIndexed(items, key = { _, c -> c.id }) { _, c ->
                        ChannelRow(c.name, c.logo, onOk = { onPlay(c.url) })
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 30.dp, top = 4.dp)
                ) {
                    itemsIndexed(items, key = { _, c -> c.id }) { _, c ->
                        PosterCard(c.name, c.logo, c.rating, width = 150.dp, onOk = { onOpen(c) })
                    }
                }
            }
        }
    }
}

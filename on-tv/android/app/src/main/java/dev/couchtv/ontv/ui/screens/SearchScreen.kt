package dev.couchtv.ontv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo
import dev.couchtv.ontv.ui.components.PosterCard
import kotlinx.coroutines.delay

/**
 * Search, the reference design's way: a heading, a pill input, Trending Today beneath it until a
 * couple of typed letters swap in results from the whole line.
 */
@Composable
fun SearchScreen(onPlay: (String) -> Unit, onOpen: (UiRepo.RailItem) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UiRepo.RailItem>>(emptyList()) }
    var trending by remember { mutableStateOf<List<UiRepo.RailItem>>(emptyList()) }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        runCatching {
            val h = UiRepo.home()
            trending = (h.movies + h.series).sortedByDescending { it.rating }.take(18)
        }
    }
    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); note = ""; return@LaunchedEffect }
        delay(350)   // let the keyboard settle before asking the index
        runCatching { results = UiRepo.search(query) ; note = if (results.isEmpty()) "Nothing matches \"$query\"." else "" }
            .onFailure { note = it.message ?: "Search did not answer." }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ready for an adventure?", style = Cj.RowTitle, modifier = Modifier.padding(top = 18.dp, bottom = 14.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = Cj.Label.copy(color = Color.White),
            cursorBrush = SolidColor(Cj.Accent),
            decorationBox = { inner ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.width(520.dp)
                        .background(Cj.Surface2.copy(alpha = 0.9f), CircleShape)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    if (query.isEmpty()) Text("⌕  Search movies, shows and channels", style = Cj.Label.copy(color = Cj.Dim))
                    inner()
                }
            }
        )
        val showing = if (results.isNotEmpty()) results else trending
        Text(
            if (results.isNotEmpty()) "Results" else if (note.isNotEmpty()) note else "Trending Today",
            style = Cj.Small, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 44.dp, end = 44.dp, bottom = 30.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(showing, key = { i, c -> c.kind + c.id + i }) { _, c ->
                PosterCard(c.name, c.logo, c.rating, width = 150.dp,
                    onOk = { if (c.kind == "live") onPlay(c.url) else onOpen(c) })
            }
        }
    }
}

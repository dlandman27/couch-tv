package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.Stars
import dev.couchtv.ontv.ui.UiRepo

/**
 * A film or series detail page over the grid, the reference design's hero-detail look: the
 * backdrop full-bleed with a dark wash, the title and facts on the left, a Play button (a film
 * plays, a series opens its episodes). Loads its own content from the id it is given.
 */
@Composable
fun DetailOverlay(
    kind: String,
    id: String,
    fallbackName: String,
    onPlay: (String) -> Unit,
    onEpisodes: () -> Unit,
    onOpen: (UiRepo.RailItem) -> Unit,
    onClose: () -> Unit
) {
    var d by remember(id) { mutableStateOf<UiRepo.Detail?>(null) }
    var related by remember(id) { mutableStateOf<List<UiRepo.RailItem>>(emptyList()) }
    var note by remember(id) { mutableStateOf("") }
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(kind, id) {
        runCatching { d = UiRepo.detail(kind, id) }.onFailure { note = it.message ?: "No detail." }
    }
    LaunchedEffect(kind, id) { runCatching { related = UiRepo.related(kind, id).take(12) } }
    LaunchedEffect(d != null) { if (d != null) runCatching { playFocus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(Cj.Ground)) {
        val det = d
        AsyncImage(
            model = (det?.backdrop ?: "").ifEmpty { det?.poster },
            contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            0f to Color(0xF2050505), 0.45f to Color(0x99050505), 0.8f to Color(0x40050505))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0.55f to Color.Transparent, 1f to Color(0xCC050505))))

        Column(Modifier.align(Alignment.CenterStart).padding(start = 56.dp, end = 40.dp, top = 60.dp).widthIn(max = 760.dp)) {
            Text(det?.name ?: fallbackName, style = Cj.HeroTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
                Stars(det?.rating ?: 0.0)
                if (!det?.year.isNullOrEmpty()) Text(det!!.year, style = Cj.Label.copy(color = Color.White))
                if (!det?.duration.isNullOrEmpty()) Text(det!!.duration, style = Cj.Label.copy(color = Color.White))
                if (!det?.genre.isNullOrEmpty()) Text(det!!.genre, style = Cj.Label.copy(color = Cj.Hi), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!det?.plot.isNullOrEmpty())
                Text(det!!.plot, style = Cj.Body, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp))
            if (!det?.cast.isNullOrEmpty())
                Text("Starring  " + det!!.cast, style = Cj.Small.copy(color = Cj.Dim), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
            if (note.isNotEmpty()) Text(note, style = Cj.Body.copy(color = Cj.Dim), modifier = Modifier.padding(top = 8.dp))

            Surface(
                onClick = { if (kind == "series") onEpisodes() else det?.url?.takeIf { it.isNotEmpty() }?.let { onPlay(it) } },
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.92f), contentColor = Cj.Ink,
                    focusedContainerColor = Color.White, focusedContentColor = Cj.Ink),
                border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Cj.Accent), shape = CircleShape)),
                modifier = Modifier.padding(top = 18.dp).focusRequester(playFocus)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 30.dp, vertical = 12.dp)) {
                    Text(if (kind == "series") "▶  Episodes" else "▶  Play", style = Cj.Label.copy(color = Cj.Ink))
                }
            }
        }

        if (related.isNotEmpty()) {
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 24.dp)) {
                Text("More like this", style = Cj.RowTitle.copy(fontSize = 21.sp),
                    modifier = Modifier.padding(start = 56.dp, bottom = 10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 56.dp)
                ) {
                    items(related, key = { it.kind + it.id }) { r ->
                        PosterCard(r.name, r.logo, r.rating, width = 120.dp, onOk = { onOpen(r) })
                    }
                }
            }
        }
    }
}

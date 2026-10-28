package dev.couchtv.ontv.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
import kotlinx.coroutines.delay

/**
 * The home hero: the newest films taking turns behind a title, stars, plot and a Play pill.
 * Crossfades every eight seconds; left and right turn it by hand while Play holds focus.
 */
@Composable
fun HeroCarousel(films: List<UiRepo.Featured>, onPlay: (UiRepo.Featured) -> Unit,
                 onUp: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    if (films.isEmpty()) return
    var at by remember { mutableIntStateOf(0) }
    var heldAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }   // a key press holds the slide a while
    val playFocus = remember { androidx.compose.ui.focus.FocusRequester() }

    // the remote has to land somewhere when the app opens: on Play
    LaunchedEffect(films.isNotEmpty()) {
        if (films.isNotEmpty()) runCatching { playFocus.requestFocus() }
    }

    LaunchedEffect(films) {
        while (true) {
            delay(8000)
            if (System.currentTimeMillis() - heldAt > 7000) at = (at + 1) % films.size
        }
    }

    // The caller sizes the hero (fillParentMaxHeight in the list), which is stable - reading the
    // screen height here made it resize when the system bars hid just after launch.
    val film = films[at % films.size]
    Box(modifier = modifier.fillMaxWidth()) {   // runs up under the floating nav
        AnimatedContent(
            targetState = film,
            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
            label = "hero"
        ) { f ->
            AsyncImage(model = f.backdrop.ifEmpty { f.poster }, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            0f to Color(0xE6050505), 0.45f to Color(0x73050505), 0.75f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0.6f to Color.Transparent, 1f to Color(0x8C050505))))

        // The hero Box is a concrete height (the caller passes it); anchoring the block to the
        // bottom here puts the title low-left, well clear of the floating nav.
        Column(Modifier.align(Alignment.BottomStart).padding(start = 44.dp, end = 40.dp, bottom = 30.dp).widthIn(max = 600.dp)) {
            Text("Picked for you", style = Cj.Small.copy(color = Color.White.copy(alpha = 0.75f)))
            Text(film.name, style = Cj.HeroTitle.copy(fontSize = 40.sp, lineHeight = 44.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                Stars(film.rating, size = 13.sp)
                if (film.year.isNotEmpty()) Text(film.year, style = Cj.Label.copy(color = Color.White))
                if (film.genre.isNotEmpty()) Text(film.genre, style = Cj.Label.copy(color = Cj.Hi), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (film.plot.isNotEmpty())
                Text(film.plot, style = Cj.Body.copy(fontSize = 15.sp, lineHeight = 21.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Surface(
                onClick = { onPlay(films[at % films.size]) },
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.92f), contentColor = Cj.Ink,
                    focusedContainerColor = Color.White, focusedContentColor = Cj.Ink),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(BorderStroke(3.dp, Cj.Accent), shape = CircleShape)),
                modifier = Modifier.padding(top = 14.dp)
                    .focusRequester(playFocus)
                    .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (e.key) {
                        Key.DirectionLeft -> { at = (at - 1 + films.size) % films.size; heldAt = System.currentTimeMillis(); true }
                        Key.DirectionRight -> { at = (at + 1) % films.size; heldAt = System.currentTimeMillis(); true }
                        Key.DirectionUp -> { onUp?.invoke(); onUp != null }
                        else -> false
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 30.dp, vertical = 12.dp)) {
                    Text("▶", style = Cj.Label.copy(color = Cj.Ink))
                    Text("  Play", style = Cj.Label.copy(color = Cj.Ink))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 30.dp)) {
            films.forEachIndexed { i, _ ->
                Box(Modifier.size(width = if (i == at % films.size) 22.dp else 8.dp, height = 8.dp)
                    .background(if (i == at % films.size) Color.White else Color.White.copy(alpha = 0.35f), CircleShape))
            }
        }
    }
}

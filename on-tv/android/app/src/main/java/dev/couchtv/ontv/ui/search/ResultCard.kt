package dev.couchtv.ontv.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.couchtv.ontv.search.Hit
import dev.couchtv.ontv.search.Kind
import dev.couchtv.ontv.search.Shape
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.components.AppIcon

/**
 * One search result, shaped by what it is: a 2:3 poster for films and shows, a 16:9 tile for
 * channels (logo on a dark ground) and games (both teams' logos), an app tile for apps. One focus
 * treatment everywhere: a crisp white outline and a restrained lift. Missing artwork falls back to
 * a dark panel carrying the title — never a stock image.
 */
@Composable
fun ResultCard(hit: Hit, onOk: () -> Unit, modifier: Modifier = Modifier, fill: Boolean = false) {
    if (hit.shape == Shape.Square) {
        Box(modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier), contentAlignment = Alignment.TopCenter) {
            AppIcon(hit.title, hit.icon, size = SearchSpec.AppSize, onOk = onOk)
        }
        return
    }
    val poster = hit.shape == Shape.Poster
    // in the results grid a card fills its column, so every column is exactly the same width
    Column(modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier.width(if (poster) SearchSpec.PosterW else SearchSpec.WideW))) {
        Surface(
            onClick = onOk,
            shape = ClickableSurfaceDefaults.shape(Cj.CardSmall),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Cj.Surface, focusedContainerColor = Cj.Surface),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)), shape = Cj.CardSmall),
                focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = Cj.CardSmall)),
            modifier = Modifier.fillMaxWidth().aspectRatio(if (poster) 2f / 3f else 16f / 9f)
        ) {
            Box(Modifier.fillMaxSize()) {
                when {
                    hit.kind == Kind.Game -> GameArt(hit)
                    hit.image.isEmpty() -> Fallback(hit.title)
                    hit.kind == Kind.Channel -> {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1E2027), Color(0xFF121318)))))
                        AsyncImage(model = hit.image, contentDescription = hit.title, contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp))
                    }
                    else -> AsyncImage(model = hit.image, contentDescription = hit.title, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                }
                hit.badge?.let { b ->
                    Text(b, style = Cj.Small.copy(color = Color.White, fontSize = 7.sp),
                        modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                            .background(Color(0xFFE5252A), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
        }
        Text(hit.title, style = Cj.Label.copy(color = Color.White.copy(alpha = 0.92f), fontSize = 9.5.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
        Text(hit.subtitle, style = Cj.Small.copy(color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A game: the away team's logo, "vs", the home team's, on a dark ground. */
@Composable
private fun GameArt(hit: Hit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1E2027), Color(0xFF121318)))))
    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        AsyncImage(model = hit.image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(34.dp))
        Text("vs", style = Cj.Small.copy(color = Color.White.copy(alpha = 0.45f), fontSize = 8.sp))
        AsyncImage(model = hit.image2, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun Fallback(title: String) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C1E24), Color(0xFF101116)))),
        contentAlignment = Alignment.Center) {
        Text(title, style = Cj.Small.copy(color = Color.White.copy(alpha = 0.55f), fontSize = 8.sp),
            maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp))
    }
}

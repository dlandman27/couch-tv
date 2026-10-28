package dev.couchtv.ontv.ui.search

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.NavIcons

/**
 * The search field in search mode: the same quiet translucent pill as Home's, now showing the
 * query and a soft caret. Typing happens on the on-screen keyboard, so the field itself is not a
 * focus stop; only the mic is. While listening the mic pulses and the field shows what's heard.
 */
@Composable
fun SearchField(
    query: String,
    listening: Boolean,
    heard: String,
    onMic: () -> Unit,
    micFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "field")
    val caret by pulse.animateFloat(1f, 0f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "caret")
    val shape = RoundedCornerShape(50)
    Box(modifier.width(SearchSpec.FieldWidth).height(40.dp)
        .background(Color(0xB30E1014), shape).border(1.dp, Color.White.copy(alpha = 0.16f), shape)) {
        Row(Modifier.align(Alignment.CenterStart).padding(start = 18.dp, end = 52.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(NavIcons.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(13.dp))
            val text = when {
                listening -> heard.ifEmpty { "Listening…" }
                query.isEmpty() -> "Search or ask Orbit"
                else -> query
            }
            val dim = listening && heard.isEmpty() || !listening && query.isEmpty()
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = Cj.Body.copy(fontSize = 12.sp, shadow = null,
                    color = if (dim) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.95f)),
                modifier = Modifier.padding(start = 11.dp))
            if (!listening) Box(Modifier.padding(start = 2.dp).width(1.dp).height(15.dp).alpha(caret)
                .background(Color.White.copy(alpha = 0.85f)))
        }
        MicButton(listening, onMic, Modifier.align(Alignment.CenterEnd).padding(end = 5.dp).focusRequester(micFocus))
    }
}

@Composable
private fun MicButton(listening: Boolean, onOk: () -> Unit, modifier: Modifier) {
    val pulse = rememberInfiniteTransition(label = "mic")
    val ring by pulse.animateFloat(1f, 1.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        if (listening) Box(Modifier.size(30.dp).scale(ring).background(Color.White.copy(alpha = 0.12f), CircleShape))
        Surface(
            onClick = onOk,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (listening) Color.White else Color.Transparent,
                contentColor = if (listening) Cj.Ink else Color.White.copy(alpha = 0.7f),
                focusedContainerColor = if (listening) Color.White else Color(0x26FFFFFF),
                focusedContentColor = if (listening) Cj.Ink else Color.White),
            border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.5.dp, Color.White), shape = CircleShape)),
            modifier = Modifier.size(30.dp)
        ) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Icon(NavIcons.Mic, contentDescription = "Voice search", modifier = Modifier.size(13.dp))
            }
        }
    }
}

/** Search screen metrics (dp; ×2 px on the 1920×1080 panel). */
object SearchSpec {
    val FieldWidth = 450.dp      // ~900px on the reference canvas
    val FieldTop = 62.dp
    const val KeyboardShare = 0.28f   // of the usable width; the results take 0.68, the gap the rest
    const val GapShare = 0.04f
    const val ResultsShare = 0.68f
    const val Columns = 5             // poster columns in the results grid
    val PosterW = 80.dp
    val WideW = 128.dp
    val AppSize = 46.dp
}

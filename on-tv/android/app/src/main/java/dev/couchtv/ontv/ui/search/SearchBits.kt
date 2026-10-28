package dev.couchtv.ontv.ui.search

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
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

/** A quiet one-line row: a suggestion, a recent search. Focus = white outline, nothing filled. */
@Composable
fun SuggestionRow(text: String, icon: ImageVector?, onOk: () -> Unit, modifier: Modifier = Modifier) {
    val shape = Cj.CardSmall
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x14FFFFFF), contentColor = Color.White.copy(alpha = 0.78f),
            focusedContainerColor = Color(0x29FFFFFF), focusedContentColor = Color.White),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.5.dp, Color.White), shape = shape)),
        modifier = modifier
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(10.dp))
            Text(text, style = Cj.Body.copy(color = Color.Unspecified, fontSize = 10.sp, shadow = null),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A type filter: the chosen one carries a soft fill, the focused one a white outline. */
@Composable
fun KindChip(label: String, selected: Boolean, onOk: () -> Unit, modifier: Modifier = Modifier) {
    val shape = Cj.CardSmall
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color(0x33FFFFFF) else Color.Transparent,
            contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.62f),
            focusedContainerColor = if (selected) Color(0x40FFFFFF) else Color(0x1AFFFFFF),
            focusedContentColor = Color.White),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = if (selected) 0f else 0.14f)), shape = shape),
            focusedBorder = Border(BorderStroke(1.5.dp, Color.White), shape = shape)),
        modifier = modifier
    ) {
        Text(label, style = Cj.Label.copy(color = Color.Unspecified, fontSize = 9.5.sp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/**
 * Listening, in place of the results (not a separate voice screen): a softly breathing mic, a
 * few bars that follow the mic level, what's been heard so far, and Cancel.
 */
@Composable
fun ListeningPanel(heard: String, level: Float, message: String, onCancel: () -> Unit, cancelFocus: FocusRequester,
                   modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "listen")
    val breathe by t.animateFloat(1f, 1.18f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "breathe")
    val wobble by t.animateFloat(0f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "wobble")
    val lvl by animateFloatAsState(level, tween(120), label = "lvl")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Bars(lvl, wobble, mirrored = true)
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(54.dp).scale(breathe).background(Color.White.copy(alpha = 0.08f), CircleShape))
                Box(Modifier.size(40.dp).background(Color.White.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(NavIcons.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Bars(lvl, wobble, mirrored = false)
        }
        Text(if (message.isNotEmpty()) message else "Listening…",
            style = Cj.Label.copy(color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp),
            modifier = Modifier.padding(top = 14.dp))
        Text(heard.ifEmpty { "Say a title, a genre, a team, or “open” an app" },
            style = Cj.Body.copy(color = Color.White.copy(alpha = if (heard.isEmpty()) 0.45f else 0.85f), fontSize = 10.sp, shadow = null),
            textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 6.dp).width(320.dp))
        SuggestionRow("Cancel", null, onCancel, Modifier.padding(top = 16.dp).focusRequester(cancelFocus))
    }
}

@Composable
private fun Bars(level: Float, wobble: Float, mirrored: Boolean) {
    val shape = RoundedCornerShape(2.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        val base = listOf(0.35f, 0.6f, 0.85f, 0.6f, 0.4f)
        (if (mirrored) base.reversed() else base).forEachIndexed { i, b ->
            val h = 4f + 22f * b * (0.25f + 0.75f * level) * (0.8f + 0.2f * if (i % 2 == 0) wobble else 1 - wobble)
            Box(Modifier.width(2.dp).height(h.dp).background(Color.White.copy(alpha = 0.55f), shape))
        }
    }
}

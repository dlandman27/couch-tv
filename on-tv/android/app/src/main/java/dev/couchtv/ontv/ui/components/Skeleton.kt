package dev.couchtv.ontv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The skeleton kit: pages mount instantly and paint these pulsing placeholders while the line
 * answers, so switching tabs never shows a blank screen or a bare "Loading…" line.
 */

/** One pulsing placeholder brick; every skeleton is built from these. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, corner: Dp = 10.dp) {
    val t = rememberInfiniteTransition(label = "skeleton")
    val alpha by t.animateFloat(
        initialValue = 0.045f, targetValue = 0.11f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse")
    Box(modifier.clip(RoundedCornerShape(corner)).background(Color.White.copy(alpha = alpha)))
}

/** A poster-shaped placeholder: a 2:3 tile with a short title bar beneath it. */
@Composable
fun PosterSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(2f / 3f), corner = 12.dp)
        SkeletonBox(Modifier.padding(top = 8.dp).fillMaxWidth(0.8f).height(11.dp), corner = 4.dp)
    }
}

/** A full grid of poster placeholders, matching the real catalog grid. */
@Composable
fun PosterGridSkeleton(columns: Int = 7, count: Int = 21, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
        modifier = modifier
    ) {
        items(count) { PosterSkeleton() }
    }
}

/** A row of pill placeholders, standing in for the category / filter chips. */
@Composable
fun ChipRowSkeleton(count: Int = 6, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            SkeletonBox(Modifier.width((70 + (i % 3) * 34).dp).height(38.dp), corner = 20.dp)
        }
    }
}

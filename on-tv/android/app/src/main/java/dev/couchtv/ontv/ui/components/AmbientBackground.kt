package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj

/**
 * The cinematic night wallpaper behind the whole OS, dimmed per screen: visible on Home/Search,
 * a faint glow behind the catalogue, near-black for the readable grids, softly blurred for Apps.
 * A near-black base always sits underneath, so a screen that wants no wallpaper just reads black.
 */
@Composable
fun AmbientBackground(wallpaper: Boolean, dim: Float, blur: Boolean = false, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Cj.Ground)) {
        if (wallpaper) {
            AsyncImage(
                // lives in assets/orbit, not assets/www: the build's copyWeb wipes www on every run
                model = "file:///android_asset/orbit/wallpaper.jpg",
                contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().then(if (blur) Modifier.blur(22.dp) else Modifier))
            // an even wash to set the overall darkness; eased, so Home → Search dims rather than jumps
            val wash by animateFloatAsState(dim, tween(320), label = "dim")
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = wash)))
            // extra shade top and bottom so the nav and the lower content stay legible over the sky/lake
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color(0x80000000), 0.30f to Color.Transparent, 0.70f to Color.Transparent, 1f to Color(0xB3000000))))
        }
    }
}

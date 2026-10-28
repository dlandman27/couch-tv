package dev.couchtv.ontv.ui.components

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.couchtv.ontv.ui.Cj

/**
 * A Browse by Provider tile: the brand's own app icon when that app is on this TV, over the
 * brand's colour bleeding across a dark card.
 */
@Composable
fun ProviderTile(id: String, name: String, onOk: () -> Unit, modifier: Modifier = Modifier) {
    val pm = LocalContext.current.packageManager
    val icon = remember(id) { providerIcon(pm, id) }
    val brand = Cj.BrandColors[id] ?: Cj.Hover
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.07f),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(12.dp))),
        modifier = modifier.size(width = 176.dp, height = 74.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(brand.copy(alpha = 0.5f), Cj.Surface)), RoundedCornerShape(12.dp))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (icon != null) AsyncImage(model = icon, contentDescription = null,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)))
                Text(name, style = Cj.Label.copy(color = Color.White, fontFamily = Cj.Display))
            }
        }
    }
}

// The installed app that carries each brand (package names verified on the device). A brand with
// no app on this TV (OSN, Prime, Crunchyroll...) shows its colour and name alone.
private val PROVIDER_PACKAGES = mapOf(
    "netflix" to listOf("com.netflix.ninja"),
    "disney" to listOf("com.disney.disneyplus"),
    "hulu" to listOf("com.hulu.plus"),
    "apple" to listOf("com.apple.atve.amazon.appletv"),
    "hbo" to listOf("com.hbo.hbonow", "com.wbd.stream"),
    "paramount" to listOf("com.cbs.ott"),
    "peacock" to listOf("com.peacock.peacockfiretv"),
    "prime" to listOf("com.amazon.avod", "com.amazon.firebat"),
    "crunchyroll" to listOf("com.crunchyroll.crunchyroid"),
    "discovery" to listOf("com.discovery.discoveryplus.firetv"),
    "starz" to listOf("com.bydeluxe.d3.android.program.starz"))

private fun providerIcon(pm: PackageManager, id: String): Drawable? {
    for (pkg in PROVIDER_PACKAGES[id].orEmpty()) {
        try { return pm.getApplicationIcon(pkg) } catch (e: Exception) {}
    }
    return null
}

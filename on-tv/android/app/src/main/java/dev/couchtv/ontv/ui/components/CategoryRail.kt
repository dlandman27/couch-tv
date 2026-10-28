package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj
import dev.couchtv.ontv.ui.UiRepo

/** The left-hand category list of the browse screens, virtualised: 235 groups cost nothing. */
@Composable
fun CategoryRail(
    cats: List<UiRepo.Category>,
    activeId: String?,
    onPick: (UiRepo.Category) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(cats, key = { _, c -> c.id }) { _, c ->
            val on = c.id == activeId
            Surface(
                onClick = { onPick(c) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (on) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Cj.Ink),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Text(c.name, style = Cj.Label.copy(color = if (on) Color.White else Color.Unspecified),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
            }
        }
    }
}

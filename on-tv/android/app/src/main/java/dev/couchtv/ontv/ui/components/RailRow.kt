package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj

/** A titled, virtualised row: only what's on screen is composed, the rest recycles. */
@Composable
fun RailRow(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    item: @Composable (Int) -> Unit
) {
    if (count == 0) return
    Column(modifier = modifier) {
        Text(title, style = Cj.RowTitle, modifier = Modifier.padding(start = 44.dp, top = 14.dp, bottom = 8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 44.dp)
        ) {
            items(count = count) { i -> item(i) }
        }
    }
}

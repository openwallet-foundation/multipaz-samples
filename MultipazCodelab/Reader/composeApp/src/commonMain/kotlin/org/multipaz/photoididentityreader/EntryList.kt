package org.multipaz.photoididentityreader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EntryList(
    modifier: Modifier = Modifier,
    title: String?,
    entries: List<@Composable () -> Unit>,
) {
    if (title != null) {
        Text(
            modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }

    for (n in entries.indices) {
        val section = entries[n]
        val isFirst = (n == 0)
        val isLast = (n == entries.size - 1)
        val rounded = 16.dp
        val firstRounded = if (isFirst) rounded else 0.dp
        val endRound = if (isLast) rounded else 0.dp
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(firstRounded, firstRounded, endRound, endRound))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                section()
            }
        }
        if (!isLast) {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

package org.multipaz.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PosScaffold(
    content: @Composable () -> Unit,
) {
    val c = PosTheme.colors
    Column(Modifier.fillMaxSize()) {
        TopBar()
        Box(Modifier.fillMaxSize().background(c.surfaceDim)) {
            content()
        }
    }
}

@Composable
private fun TopBar() {
    val c = PosTheme.colors
    val type = PosTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(c.surface)
            .border(1.dp, c.surfaceVariant)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.PointOfSale,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            "UTOPIA POS TERMINAL",
            style = type.headlineMd.copy(letterSpacing = 2.sp),
            color = c.primary,
        )
    }
}
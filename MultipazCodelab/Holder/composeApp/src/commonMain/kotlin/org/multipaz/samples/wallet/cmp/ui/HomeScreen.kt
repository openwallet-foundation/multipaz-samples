package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.multipaz.document.DocumentStore
import org.multipaz.samples.wallet.cmp.util.hasAnyUsableCredential
import org.multipaz.util.Logger

private const val TAG = "AccountScreen"

@Composable
fun HomeScreen(
    documentStore: DocumentStore = koinInject(),
) {
    var selectedTabRow by remember { mutableStateOf(1) }
    val tabs = listOf("Explore", "Account")
    var hasCredentials by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val hasCred = documentStore.hasAnyUsableCredential()
        hasCredentials = hasCred
        Logger.i(TAG, "AccountScreen: hasAnyUsableCredential: $hasCred")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            TabRow(
                selectedTabIndex = selectedTabRow,
                divider = {},
                modifier = Modifier
                    .navigationBarsPadding()
                    .background(Color.White)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabRow == index,
                        onClick = { selectedTabRow = index },
                        text = { Text(title) },
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.Explore else Icons.Default.AccountCircle,
                                contentDescription = title,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            when (selectedTabRow) {
                0 -> ExploreScreen()
                1 -> AccountScreen(
                    hasCredentials = hasCredentials,
                )
            }
        }
    }
}
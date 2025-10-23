package org.multipaz.photoididentityreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.multipaz.compose.permissions.rememberBluetoothPermissionState

@Composable
fun SelectRequestScreen(
    readerModel: ReaderModel,
    settingsModel: SettingsModel,
    readerBackendClient: ReaderBackendClient,
    onBackPressed: () -> Unit,
    onContinueClicked: () -> Unit,
    onReaderIdentitiesClicked: () -> Unit
) {
    val selectedQueryName = remember { mutableStateOf(
        ReaderQuery.valueOf(settingsModel.selectedQueryName.value).name
    )}
    val blePermissionState = rememberBluetoothPermissionState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppBar(
                title = null,
                onBackPressed = onBackPressed,
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!blePermissionState.isGranted) {
                RequestBluetoothPermission(blePermissionState)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.weight(0.3f))

                    Text(
                        text = "Select what to request",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(0.1f))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(
                            16.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        for (query in ReaderQuery.entries) {
                            FilterChip(
                                selected = query.name == selectedQueryName.value,
                                onClick = {
                                    selectedQueryName.value = query.name
                                    settingsModel.selectedQueryName.value = query.name
                                },
                                label = { Text(text = query.displayName) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(0.2f))

                    RequestingData(
                        settingsModel = settingsModel,
                        onClicked = onReaderIdentitiesClicked
                    )

                    Spacer(modifier = Modifier.weight(0.2f))

                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                val readerQuery = ReaderQuery.valueOf(settingsModel.selectedQueryName.value)
                                readerModel.setDeviceRequest(
                                    readerQuery.generateDeviceRequest(
                                        settingsModel = settingsModel,
                                        encodedSessionTranscript = readerModel.encodedSessionTranscript,
                                        readerBackendClient = readerBackendClient
                                    ),
                                )
                                onContinueClicked()
                            }
                        }
                    ) {
                        Text(
                            text = "Continue"
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.1f))
                }
            }
        }
    }
}
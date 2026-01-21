package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import kotlinx.coroutines.launch
import org.multipaz.compose.document.DocumentInfo
import org.multipaz.compose.document.DocumentModel
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.util.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailScreen(
    documentInfo: DocumentInfo,
    documentModel: DocumentModel,
    presentmentModel: PresentmentModel,
    presentmentSource: PresentmentSource,
    documentTypeRepository: DocumentTypeRepository,
    imageLoader: ImageLoader,
    onBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val blePermissionState = rememberBluetoothPermissionState()
    val bleEnabledState = rememberBluetoothEnabledState()
    var pendingQrRequest by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showBleInfoDialog by remember { mutableStateOf(false) }
    var firstAttemptToEnableBle by remember { mutableStateOf(false) }
    val documentInfo = documentModel.documentInfos
        .collectAsState().value[documentInfo.document.identifier]

    LaunchedEffect(
        pendingQrRequest,
        blePermissionState.isGranted,
        bleEnabledState.isEnabled
    ) {
        Logger.d("permission", "${blePermissionState.isGranted}")
        Logger.d("permissionEnables", "${bleEnabledState.isEnabled}")
        if (pendingQrRequest && blePermissionState.isGranted && bleEnabledState.isEnabled) {
            showQrDialog = true
            pendingQrRequest = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {  },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {

                            coroutineScope.launch {
                                when {
                                    !blePermissionState.isGranted -> {
                                        if (firstAttemptToEnableBle) {
                                            showBleInfoDialog = true
                                            if(pendingQrRequest) {
                                                pendingQrRequest = false
                                            }
                                        } else {
                                            pendingQrRequest = true
                                            blePermissionState.launchPermissionRequest()
                                            firstAttemptToEnableBle = true
                                        }

                                    }

                                    !bleEnabledState.isEnabled -> {
                                        pendingQrRequest = true
                                        bleEnabledState.enable()
                                        firstAttemptToEnableBle = true
                                    }

                                    else -> {
                                        showQrDialog = true
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.QrCode, contentDescription = "Present")
                    }
                }
            )
        }
    ) { padding ->
        documentInfo?.let { info ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                DocumentCard(info)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Contactless,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Hold to reader",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }


    }

    if (showQrDialog) {
        QrPresentmentDialog(
            presentmentModel = presentmentModel,
            presentmentSource = presentmentSource,
            documentTypeRepository = documentTypeRepository,
            imageLoader = imageLoader,
            onDismiss = {
                presentmentModel.reset()
                showQrDialog = false
            }
        )
    }
    if (showBleInfoDialog) {
        BleSettingsDialog(
            onDismiss = { showBleInfoDialog = false }
        )
    }
}

@Composable
fun DocumentCard(
    documentInfo: DocumentInfo
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Image(
            bitmap = documentInfo.cardArt,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun BleSettingsDialog(
    onDismiss: () -> Unit
) {
   AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bluetooth required") },
        text = {
            Text(
                "To present your ID, enable Bluetooth permissions and Bluetooth access in Settings."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {

                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },

    )
}





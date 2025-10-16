package org.multipaz.photoididentityreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import multipazphotoididentityreader.composeapp.generated.resources.Res
import multipazphotoididentityreader.composeapp.generated.resources.scan_qr_title
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.camera.CameraCaptureResolution
import org.multipaz.compose.camera.CameraSelection
import org.multipaz.compose.permissions.rememberCameraPermissionState
import org.multipaz.compose.qrcode.QrCodeScanner

@Composable
fun ScanQrScreen(
    onBackPressed: () -> Unit,
    onMdocQrCodeScanned: (mdocUri: String) -> Unit
) {
    val cameraPermissionState = rememberCameraPermissionState()
    val coroutineScope = rememberCoroutineScope()
    var invokedCallback by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppBar(
                title = AnnotatedString(stringResource(Res.string.scan_qr_title)),
                onBackPressed = onBackPressed,
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Ask the person presenting to show a QR code." +
                            "The QR code usually available in the Wallet app holding the document " +
                            "they wish to present.",
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!cameraPermissionState.isGranted) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        cameraPermissionState.launchPermissionRequest()
                                    }
                                }
                            ) {
                                Text("Request Camera permission")
                            }
                        }
                    } else {
                        QrCodeScanner(
                            modifier = Modifier.fillMaxSize().clipToBounds(),
                            cameraSelection = CameraSelection.DEFAULT_BACK_CAMERA,
                            captureResolution = CameraCaptureResolution.HIGH,
                            showCameraPreview = true,
                            onCodeScanned = { qrCodeUri ->
                                if (qrCodeUri != null && qrCodeUri.startsWith("mdoc:") && !invokedCallback) {
                                    invokedCallback = true
                                    coroutineScope.launch {
                                        onMdocQrCodeScanned(qrCodeUri)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
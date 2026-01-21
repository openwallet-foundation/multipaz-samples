package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import mpzcmpwallet.composeapp.generated.resources.Res
import mpzcmpwallet.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.samples.wallet.cmp.App
import org.multipaz.util.UUID

@Composable
fun QrPresentmentDialog(
    presentmentModel: PresentmentModel,
    presentmentSource: PresentmentSource,
    documentTypeRepository: DocumentTypeRepository,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = {
            presentmentModel.reset()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp)
        ) {
            MdocProximityQrPresentment(
                appName = "MpzCmpWallet",
                appIconPainter = painterResource(Res.drawable.compose_multiplatform),
                presentmentModel = presentmentModel,
                presentmentSource = presentmentSource,
                promptModel = App.promptModel,
                documentTypeRepository = documentTypeRepository,
                imageLoader = imageLoader,
                allowMultipleRequests = false,
                showQrButton = { onQrButtonClicked -> ShowQrButton(onQrButtonClicked) },
                showQrCode = { uri ->
                    ShowQrCode(
                        uri = uri,
                        onDismiss = {
                            presentmentModel.reset()
                            onDismiss()
                        }
                    )
                }

            )
        }
    }
}


@Composable
private fun ShowQrButton(
    onQrButtonClicked: (settings: MdocProximityQrSettings) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Present mDL",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val connectionMethods = listOf(
                    MdocConnectionMethodBle(
                        supportsPeripheralServerMode = false,
                        supportsCentralClientMode = true,
                        peripheralServerModeUuid = null,
                        centralClientModeUuid = UUID.randomUUID(),
                    )
                )
                onQrButtonClicked(
                    MdocProximityQrSettings(
                        availableConnectionMethods = connectionMethods,
                        createTransportOptions = MdocTransportOptions(
                            bleUseL2CAP = true
                        )
                    )
                )
            }
        ) {
            Text("Show QR code")
        }
    }
}

@Composable
private fun ShowQrCode(
    uri: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val qrCodeBitmap = remember { generateQrCode(uri) }
        Text(
            text = "Present QR code to mdoc reader",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.height(16.dp))

        Image(
            modifier = Modifier.fillMaxWidth(),
            bitmap = qrCodeBitmap,
            contentDescription = null,
            contentScale = ContentScale.FillWidth
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                    onDismiss()
            }
        ) {
            Text("Cancel")
        }
    }
}
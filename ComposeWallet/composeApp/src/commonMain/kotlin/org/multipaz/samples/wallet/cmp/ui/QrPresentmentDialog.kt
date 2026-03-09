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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import mpzcmpwallet.composeapp.generated.resources.Res
import mpzcmpwallet.composeapp.generated.resources.cancel
import mpzcmpwallet.composeapp.generated.resources.compose_multiplatform
import mpzcmpwallet.composeapp.generated.resources.present_mdl
import mpzcmpwallet.composeapp.generated.resources.present_qr_to_reader
import mpzcmpwallet.composeapp.generated.resources.show_qr_code
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.samples.wallet.cmp.App
import org.multipaz.util.UUID
import kotlin.time.Duration.Companion.seconds

@Composable
fun QrPresentmentDialog(
    presentmentSource: PresentmentSource,
    promptModel: PromptModel,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = {
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
                source = presentmentSource,
                promptModel = promptModel,
                prepareSettings = { generateQrCode ->
                    val connectionMethods = mutableListOf<MdocConnectionMethod>()
                    val bleUuid = UUID.randomUUID()
                    connectionMethods.add(
                        MdocConnectionMethodBle(
                            supportsPeripheralServerMode = true,
                            supportsCentralClientMode = false,
                            peripheralServerModeUuid = bleUuid,
                            centralClientModeUuid = null,
                        )
                    )
                    generateQrCode(
                        MdocProximityQrSettings(
                            availableConnectionMethods = connectionMethods,
                            createTransportOptions = MdocTransportOptions(bleUseL2CAPInEngagement = true)
                        )
                    )
                },
                showQrCode = { uri, reset ->
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val qrCodeBitmap = remember { generateQrCode(uri) }
                        Text(
                            text = stringResource(Res.string.present_qr_to_reader),
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
                        Button(onClick = { reset() }) {
                            Text(stringResource(Res.string.cancel))
                        }
                    }
                },
                showTransacting = { reset ->
                    Text("Transacting")
                    Button(onClick = { reset() }) {
                        Text("Cancel")
                    }
                },
                showCompleted = { error, reset ->
                    if (error is CancellationException) {
                        onDismiss()
                    } else {
                        if (error != null) {
                            Text("Something went wrong: $error")
                        } else {
                            Text("The data was shared")
                        }
                        LaunchedEffect(Unit) {
                            delay(1.5.seconds)
                            onDismiss()
                        }
                    }
                },
            )
        }
    }
}

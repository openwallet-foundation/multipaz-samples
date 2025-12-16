package org.multipaz.getstarted

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonObject
import org.multipaz.cbor.DataItem
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.X509CertChain
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.getstarted.w3cdc.ShowResponse
import org.multipaz.getstarted.w3cdc.ShowResponseMetadata

@Composable
fun ShowResponseScreen(
    vpToken: JsonObject?,
    deviceResponse: DataItem?,
    sessionTranscript: DataItem,
    nonce: ByteString?,
    eReaderKey: EcPrivateKey?,
    metadata: ShowResponseMetadata,
    documentTypeRepository: DocumentTypeRepository?,
    onViewCertChain: (certChain: X509CertChain) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth().padding(8.dp)
            .verticalScroll(scrollState)
    ) {
        ShowResponse(
            vpToken = vpToken,
            deviceResponse = deviceResponse,
            sessionTranscript = sessionTranscript,
            nonce = nonce,
            eReaderKey = eReaderKey,
            metadata = metadata,
            documentTypeRepository = documentTypeRepository,
            onViewCertChain = onViewCertChain
        )
    }
}
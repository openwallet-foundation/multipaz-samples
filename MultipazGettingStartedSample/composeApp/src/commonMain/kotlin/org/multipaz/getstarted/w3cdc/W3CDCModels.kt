package org.multipaz.getstarted.w3cdc

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.CborMap
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tstr
import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.cbor.toDataItem
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.documenttype.DocumentCannedRequest
import org.multipaz.documenttype.DocumentType
import org.multipaz.getstarted.Destination
import org.multipaz.util.toBase64Url

/* Models used for W3C DC Native Flow */

@CborSerializable
data class ShowResponseMetadata(
    val engagementType: String,
    val transferProtocol: String,
    val requestSize: Long,
    val responseSize: Long,
    val durationMsecNfcTapToEngagement: Long?,
    val durationMsecEngagementReceivedToRequestSent: Long?,
    val durationMsecRequestSentToResponseReceived: Long
)

data class RequestEntry(
    val displayName: String,
    val documentType: DocumentType,
    val sampleRequest: DocumentCannedRequest
)

/* Helper functions used for W3C DC Native Flow */

fun ShowResponseMetadata.toDataItem(): DataItem {
    val builder = CborMap.builder()
    builder.put("engagementType", Tstr(this.engagementType))
    builder.put("transferProtocol", Tstr(this.transferProtocol))
    builder.put("requestSize", this.requestSize.toDataItem())
    builder.put("responseSize", this.responseSize.toDataItem())
    val durationMsecNfcTapToEngagement = this.durationMsecNfcTapToEngagement
    if (durationMsecNfcTapToEngagement != null) {
        builder.put("durationMsecNfcTapToEngagement", durationMsecNfcTapToEngagement.toDataItem())
    }
    val durationMsecEngagementReceivedToRequestSent =
        this.durationMsecEngagementReceivedToRequestSent
    if (durationMsecEngagementReceivedToRequestSent != null) {
        builder.put(
            "durationMsecEngagementReceivedToRequestSent",
            durationMsecEngagementReceivedToRequestSent.toDataItem()
        )
    }
    builder.put(
        "durationMsecRequestSentToResponseReceived",
        this.durationMsecRequestSentToResponseReceived.toDataItem()
    )
    return builder.end().build()
}

fun buildShowResponseDestination(
    vpToken: JsonObject?,
    deviceResponse: DataItem?,
    sessionTranscript: DataItem,
    nonce: ByteString?,
    eReaderKey: EcPrivateKey?,
    metadata: ShowResponseMetadata
): Destination.ShowResponseDestination {

    fun JsonObject?.jsonBase64() =
        this?.let { Json.encodeToString(it).encodeToByteArray().toBase64Url() }

    fun DataItem?.cborBase64() =
        this?.let { Cbor.encode(it).toBase64Url() }

    fun DataItem.cborBase64Required() =
        Cbor.encode(this).toBase64Url()

    fun ByteString?.base64() =
        this?.toByteArray()?.toBase64Url()

    fun EcPrivateKey?.coseKeyBase64() =
        this
            ?.toCoseKey()
            ?.toDataItem()
            ?.let { Cbor.encode(it).toBase64Url() }

    return Destination.ShowResponseDestination(
        vpResponse = vpToken.jsonBase64(),
        deviceResponse = deviceResponse.cborBase64(),
        sessionTranscript = sessionTranscript.cborBase64Required(),
        nonce = nonce.base64(),
        eReaderKey = eReaderKey.coseKeyBase64(),
        metadata = metadata.toDataItem().cborBase64Required()
    )
}
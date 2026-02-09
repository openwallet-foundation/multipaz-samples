package org.multipaz.getstarted.w3cdc

import org.multipaz.cbor.CborMap
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tstr
import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.cbor.toDataItem

@CborSerializable
data class ShowResponseMetadata(
    val engagementType: String,
    val transferProtocol: String,
    val requestSize: Long,
    val responseSize: Long,
    val durationMsecNfcTapToEngagement: Long?,
    val durationMsecEngagementReceivedToRequestSent: Long?,
    val durationMsecRequestSentToResponseReceived: Long
) {
    companion object
}

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
    val durationMsecEngagementReceivedToRequestSent = this.durationMsecEngagementReceivedToRequestSent
    if (durationMsecEngagementReceivedToRequestSent != null) {
        builder.put("durationMsecEngagementReceivedToRequestSent", durationMsecEngagementReceivedToRequestSent.toDataItem())
    }
    builder.put("durationMsecRequestSentToResponseReceived", this.durationMsecRequestSentToResponseReceived.toDataItem())
    return builder.end().build()
}

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

fun ShowResponseMetadata.Companion.fromDataItem(dataItem: DataItem): ShowResponseMetadata {
    val engagementType = dataItem["engagementType"].asTstr
    val transferProtocol = dataItem["transferProtocol"].asTstr
    val requestSize = dataItem["requestSize"].asNumber
    val responseSize = dataItem["responseSize"].asNumber
    val durationMsecNfcTapToEngagement0 = if (dataItem.hasKey("durationMsecNfcTapToEngagement")) {
        dataItem["durationMsecNfcTapToEngagement"].asNumber
    } else {
        null
    }
    val durationMsecEngagementReceivedToRequestSent0 = if (dataItem.hasKey("durationMsecEngagementReceivedToRequestSent")) {
        dataItem["durationMsecEngagementReceivedToRequestSent"].asNumber
    } else {
        null
    }
    val durationMsecRequestSentToResponseReceived = dataItem["durationMsecRequestSentToResponseReceived"].asNumber
    return ShowResponseMetadata(
        engagementType = engagementType,
        transferProtocol = transferProtocol,
        requestSize = requestSize,
        responseSize = responseSize,
        durationMsecNfcTapToEngagement = durationMsecNfcTapToEngagement0,
        durationMsecEngagementReceivedToRequestSent = durationMsecEngagementReceivedToRequestSent0,
        durationMsecRequestSentToResponseReceived = durationMsecRequestSentToResponseReceived,
    )
}
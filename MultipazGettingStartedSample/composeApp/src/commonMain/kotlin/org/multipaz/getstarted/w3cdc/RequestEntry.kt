package org.multipaz.getstarted.w3cdc

import org.multipaz.documenttype.DocumentCannedRequest
import org.multipaz.documenttype.DocumentType

data class RequestEntry(
    val displayName: String,
    val documentType: DocumentType,
    val sampleRequest: DocumentCannedRequest
)

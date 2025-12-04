package org.multipaz.samples.wallet.cmp.util

import org.multipaz.document.DocumentStore
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
suspend fun DocumentStore.hasAnyUsableCredential(
): Boolean {
    if (listDocuments().isEmpty()) return false
    val documentId = listDocuments().first()
    val document = lookupDocument(documentId) ?: return false
    return document.hasUsableCredential()
}
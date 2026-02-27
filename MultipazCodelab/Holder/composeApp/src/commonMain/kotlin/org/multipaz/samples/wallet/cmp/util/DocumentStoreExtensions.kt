package org.multipaz.samples.wallet.cmp.util

import org.multipaz.document.DocumentStore
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
suspend fun DocumentStore.hasAnyUsableCredential(): Boolean {
    val list = listDocuments()
    if (list.isEmpty()) return false
    return list.first().hasUsableCredential()
}

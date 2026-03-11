package org.multipaz.samples.wallet.cmp

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Wallet : AppRoute
}

@Serializable
sealed interface WalletRoute {
    @Serializable
    data object WalletList : WalletRoute

    @Serializable
    data class WalletDetails(val documentId: String) : WalletRoute

    @Serializable
    data class DocumentDetails(val documentId: String) : WalletRoute

    @Serializable
    data class PersonalIdInfo(val documentId: String) : WalletRoute
}

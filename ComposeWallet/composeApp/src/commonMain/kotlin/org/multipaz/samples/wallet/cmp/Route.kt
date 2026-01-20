package org.multipaz.samples.wallet.cmp

import org.multipaz.compose.document.DocumentInfo

sealed interface AppRoute {
    data object Wallet : AppRoute
    data object Provisioning : AppRoute
}

sealed interface WalletRoute {
    data object Wallet : WalletRoute
    data class WalletDetails(val documentInfo: DocumentInfo) : WalletRoute
}


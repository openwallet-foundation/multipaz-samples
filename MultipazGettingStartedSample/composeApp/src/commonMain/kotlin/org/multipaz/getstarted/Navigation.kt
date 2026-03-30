package org.multipaz.getstarted

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {

    @Serializable
    data object HomeDestination : Destination
    @Serializable
    data object ProvisioningDestination : Destination
}

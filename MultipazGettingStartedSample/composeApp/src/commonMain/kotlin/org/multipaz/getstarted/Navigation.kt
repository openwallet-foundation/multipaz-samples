package org.multipaz.getstarted

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed interface Destination {
    val route: String

    data object HomeDestination : Destination {
        override val route = "home"
    }

    data object ProvisioningDestination : Destination {
        override val route = "provisioning"
    }

    data object ShowResponseDestination : Destination {
        override val route = "show_response"
        const val VP_TOKEN = "vp_token_arg"
        const val DEVICE_RESPONSE = "device_response_arg"
        const val SESSION_TRANSCRIPT = "session_transcript_arg"
        const val NONCE = "nonce_arg"
        const val EREADERKEY = "ereaderkey_arg"
        const val METADATA = "metadata_arg"
        val routeWithArgs = "$route/{$VP_TOKEN}/{$DEVICE_RESPONSE}/{$SESSION_TRANSCRIPT}/{$NONCE}/{$EREADERKEY}/{$METADATA}"
        val arguments = listOf(
            navArgument(VP_TOKEN) { type = NavType.StringType },
            navArgument(DEVICE_RESPONSE) { type = NavType.StringType },
            navArgument(SESSION_TRANSCRIPT) { type = NavType.StringType },
            navArgument(NONCE) { type = NavType.StringType },
            navArgument(EREADERKEY) { type = NavType.StringType },
            navArgument(METADATA) { type = NavType.StringType },
        )
    }
    data object CertificateViewerDestination : Destination {
        override val route = "certificate_details_viewer"
        const val CERTIFICATE_DATA = "certificate_data_arg"
        val routeWithArgs = "$route/{$CERTIFICATE_DATA}"
        val arguments = listOf(
            navArgument(CERTIFICATE_DATA) { type = NavType.StringType },
        )
    }
}

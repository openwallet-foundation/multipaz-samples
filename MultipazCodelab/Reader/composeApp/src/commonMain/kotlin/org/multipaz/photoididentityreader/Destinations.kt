package org.multipaz.photoididentityreader

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed interface Destination {
    val route: String
}

data object StartDestination : Destination {
    override val route = "start"
}

data object ScanQrDestination : Destination {
    override val route = "scan_qr"
}

data object SelectRequestDestination : Destination {
    override val route = "select_request"
}

data object TransferDestination : Destination {
    override val route = "transfer"
}

data object ShowResultsDestination : Destination {
    override val route = "show_results"
}

data object ShowDetailedResultsDestination : Destination {
    override val route = "show_detailed_results"
}

data object AboutDestination : Destination {
    override val route = "about"
}

data object CertificateViewerDestination : Destination {
    override val route = "certificate_viewer"
    const val CERTIFICATE_DATA_BASE64 = "certificate_data_base64_arg"
    val routeWithArgs = "$route/{$CERTIFICATE_DATA_BASE64}"
    val arguments = listOf(
        navArgument(CERTIFICATE_DATA_BASE64) { type = NavType.StringType },
    )
}

const val TRUST_MANAGER_ID_BUILT_IN = "built-in"
const val TRUST_MANAGER_ID_USER = "user"

data object TrustEntryViewerDestination : Destination {
    override val route = "trust_entry_viewer"
    const val TRUST_MANAGER_ID = "trust_manager_id"
    const val ENTRY_INDEX = "entry_index"
    const val JUST_IMPORTED = "just_imported"
    val routeWithArgs = "$route/{$TRUST_MANAGER_ID}/{$ENTRY_INDEX}/{$JUST_IMPORTED}"
    val arguments = listOf(
        navArgument(TRUST_MANAGER_ID) { type = NavType.StringType },
        navArgument(ENTRY_INDEX) { type = NavType.IntType },
        navArgument(JUST_IMPORTED) { type = NavType.BoolType },
    )
}

data object TrustEntryEditorDestination : Destination {
    override val route = "trust_entry_editor"
    const val ENTRY_INDEX = "entry_index"
    val routeWithArgs = "$route/{$ENTRY_INDEX}"
    val arguments = listOf(
        navArgument(ENTRY_INDEX) { type = NavType.IntType },
    )
}

data object VicalEntryViewerDestination : Destination {
    override val route = "vical_entry_viewer"
    const val TRUST_MANAGER_ID = "trust_manager_id"
    const val ENTRY_INDEX = "entry_index"
    const val CERTIFICATE_INDEX = "certificate_index"
    val routeWithArgs = "$route/{$TRUST_MANAGER_ID}/{$ENTRY_INDEX}/{$CERTIFICATE_INDEX}"
    val arguments = listOf(
        navArgument(TRUST_MANAGER_ID) { type = NavType.StringType },
        navArgument(ENTRY_INDEX) { type = NavType.IntType },
        navArgument(CERTIFICATE_INDEX) { type = NavType.IntType },
    )
}

data object TrustedIssuersDestination : Destination {
    override val route = "trusted_issuers"
}

data object DeveloperSettingsDestination : Destination {
    override val route = "developer_settings"
}

data object ReaderIdentityDestination : Destination {
    override val route = "reader_identity"
}

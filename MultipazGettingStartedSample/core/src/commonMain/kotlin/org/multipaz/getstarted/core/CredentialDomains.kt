package org.multipaz.getstarted.core

import org.multipaz.storage.StorageTableSpec

object CredentialDomains {
    const val MDOC_USER_AUTH = "mdoc_user_auth"
    const val MDOC_MAC_USER_AUTH = "mdoc_mac_user_auth"
    const val SDJWT_USER_AUTH = "sdjwt_user_auth"
    const val SDJWT_KEYLESS = "sdjwt_keyless"
    const val STORAGE_TABLE_NAME = "TestAppKeys"
    val storageTableSpec = StorageTableSpec(
        name = STORAGE_TABLE_NAME,
        supportPartitions = false,
        supportExpiration = false
    )

    const val SAMPLE_DOCUMENT_DISPLAY_NAME = "Erika's Driving License"
    const val SAMPLE_DOCUMENT_TYPE_DISPLAY_NAME = "Utopia Driving License"
}

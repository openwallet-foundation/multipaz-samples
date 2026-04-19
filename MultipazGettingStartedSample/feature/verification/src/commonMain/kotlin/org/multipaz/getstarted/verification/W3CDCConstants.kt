package org.multipaz.getstarted.verification

import org.multipaz.crypto.EcCurve

class W3CDCConstants {
    companion object {
        const val STORAGE_KEY_READER_ROOT_PRIVATE_KEY = "readerRootKey"
        const val STORAGE_KEY_READER_ROOT_CERT = "readerRootCert"
        const val STORAGE_KEY_READER_PRIVATE_KEY = "readerKey"
        const val STORAGE_KEY_READER_CERT = "readerCert"

        const val CERT_VALID_FROM_DATE = "2024-12-01"
        const val CERT_VALID_UNTIL_DATE = "2034-12-01"

        const val CERT_SUBJECT_COMMON_NAME = "CN=OWF Multipaz Getting Started Reader Cert"

        const val CERT_CRL_URL =
            "https://github.com/openwallet-foundation-labs/identity-credential/crl"

        const val CERT_SERIAL_NUMBER_BITS = 128

        val READER_KEY_CURVE = EcCurve.P256

        const val NONCE_SIZE_BYTES = 16

        val RESPONSE_ENCRYPTION_CURVE = EcCurve.P256

        const val METADATA_ENGAGEMENT_TYPE = "OS-provided CredentialManager API"
        const val METADATA_TRANSFER_PROTOCOL_PREFIX = "W3C Digital Credentials"
    }
}

package org.multipaz.getstarted.w3cdc

import org.multipaz.crypto.EcCurve

/**
 * Constants used for the W3C DC Native Flow
 */
class W3CDCConstants {
    companion object {
        /* Storage key names for persisting different cryptographic materials */
        const val STORAGE_KEY_READER_ROOT_PRIVATE_KEY = "readerRootKey"
        const val STORAGE_KEY_READER_ROOT_CERT = "readerRootCert"
        const val STORAGE_KEY_READER_PRIVATE_KEY = "readerKey"
        const val STORAGE_KEY_READER_CERT = "readerCert"

        /* Certificate validity dates (10-year validity period) */
        const val CERT_VALID_FROM_DATE = "2024-12-01"
        const val CERT_VALID_UNTIL_DATE = "2034-12-01"

        /* The Common Name (CN) that appears in X.509 certificates */
        const val CERT_SUBJECT_COMMON_NAME = "CN=OWF Multipaz Getting Started Reader Cert"

        /* The CRL (Certificate Revocation List) URL for certificate validation */
        const val CERT_CRL_URL =
            "https://github.com/openwallet-foundation-labs/identity-credential/crl"

        /* The number of bits for certificate serial numbers (128 bits = 16 bytes) */
        const val CERT_SERIAL_NUMBER_BITS = 128

        /* The elliptic curve used for reader key generation (P-256 is industry standard) */
        val READER_KEY_CURVE = EcCurve.P256

        /* The elliptic curve used for the bundled reader root key (P-384 provides higher security) */
        val READER_ROOT_KEY_CURVE = EcCurve.P384

        /* Nonce size in bytes for request/response correlation */
        const val NONCE_SIZE_BYTES = 16

        /* Response encryption configuration */
        val RESPONSE_ENCRYPTION_CURVE = EcCurve.P256

        /* Descriptive labels used in response metadata for logging and analytics */
        const val METADATA_ENGAGEMENT_TYPE = "OS-provided CredentialManager API"
        const val METADATA_TRANSFER_PROTOCOL_PREFIX = "W3C Digital Credentials"
    }
}
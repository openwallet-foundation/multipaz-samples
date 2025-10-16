package org.multipaz.photoididentityreader

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.RawCbor
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.LoyaltyID
import org.multipaz.documenttype.knowntypes.PhotoID
import org.multipaz.mdoc.request.DocRequestInfo
import org.multipaz.mdoc.request.buildDeviceRequest
import org.multipaz.securearea.SecureArea
import org.multipaz.util.Logger

private const val TAG = "ReaderQuery"

enum class ReaderQuery(
    val icon: ImageVector,
    val displayName: String,
) {
    /*AGE_OVER_18(
        icon = Icons.Filled.Numbers,
        displayName = "Age Over 18",
    ),
    AGE_OVER_21(
        icon = Icons.Filled.Numbers,
        displayName = "Age Over 21",
    ),
    IDENTIFICATION(
        icon = Icons.Filled.Person,
        displayName = "Identification",
    ),*/
    VALID_MEMBERSHIP_CARD(
        icon = Icons.Filled.Person,
        displayName = "Valid Membership Card",
    ),
//    PHOTO_ID(
//        icon = Icons.Filled.Person,
//        displayName = "All Membership Card Data",
//    ),

    ;

    suspend fun generateDeviceRequest(
        settingsModel: SettingsModel,
        encodedSessionTranscript: ByteString,
        readerBackendClient: ReaderBackendClient
    ): ByteString {
        val readerIdentityId = when (settingsModel.readerAuthMethod.value) {
            ReaderAuthMethod.NO_READER_AUTH,
            ReaderAuthMethod.CUSTOM_KEY,
            ReaderAuthMethod.STANDARD_READER_AUTH -> null
            ReaderAuthMethod.STANDARD_READER_AUTH_WITH_GOOGLE_ACCOUNT_DETAILS -> ""
            ReaderAuthMethod.IDENTITY_FROM_GOOGLE_ACCOUNT ->  {
                settingsModel.readerAuthMethodGoogleIdentity.value!!.id
            }
        }
        val deviceRequest = when (settingsModel.readerAuthMethod.value) {
            ReaderAuthMethod.NO_READER_AUTH -> {
                generateEncodedDeviceRequest(
                    query = this,
                    intentToRetain = settingsModel.logTransactions.value,
                    encodedSessionTranscript = encodedSessionTranscript.toByteArray(),
                    readerKey = null,
                    readerCert = null,
                    readerRootCert = null
                )
            }
            ReaderAuthMethod.IDENTITY_FROM_GOOGLE_ACCOUNT,
            ReaderAuthMethod.STANDARD_READER_AUTH,
            ReaderAuthMethod.STANDARD_READER_AUTH_WITH_GOOGLE_ACCOUNT_DETAILS -> {
                val (keyInfo, keyCertification) = try {
                    readerBackendClient.getKey(readerIdentityId)
                } catch (e: ReaderIdentityNotAvailableException) {
                    try {
                        Logger.w(TAG, "The reader identity we're configured for is no longer working", e)
                        Logger.i(TAG, "Resetting configuration to standard reader auth")
                        settingsModel.readerAuthMethod.value = ReaderAuthMethod.STANDARD_READER_AUTH
                        settingsModel.readerAuthMethodGoogleIdentity.value = null
                        readerBackendClient.getKey(null)
                    } catch (e: Throwable) {
                        Logger.e(TAG, "Error getting certified reader key, proceeding without reader authentication", e)
                        Pair(null, null)
                    }
                } catch (e: Throwable) {
                    Logger.e(TAG, "Error getting certified reader key, proceeding without reader authentication", e)
                    Pair(null, null)
                }
                generateEncodedDeviceRequest(
                    query = this,
                    intentToRetain = settingsModel.logTransactions.value,
                    encodedSessionTranscript = encodedSessionTranscript.toByteArray(),
                    readerKey = null,
                    readerCert = keyCertification?.certificates?.get(0),
                    readerRootCert = keyCertification?.certificates?.get(1)
                ).also {
                    keyInfo?.let { readerBackendClient.markKeyAsUsed(it) }
                }
            }
            ReaderAuthMethod.CUSTOM_KEY -> {
                generateEncodedDeviceRequest(
                    query = this,
                    intentToRetain = settingsModel.logTransactions.value,
                    encodedSessionTranscript = encodedSessionTranscript.toByteArray(),
                    readerKey = settingsModel.customReaderAuthKey.value!!,
                    readerCert = settingsModel.customReaderAuthCertChain.value!!.certificates[0],
                    readerRootCert = settingsModel.customReaderAuthCertChain.value!!.certificates[1]
                )
            }
        }
        return ByteString(deviceRequest)
    }
}

suspend fun generateEncodedDeviceRequest(
    query: ReaderQuery,
    intentToRetain: Boolean,
    encodedSessionTranscript: ByteArray,
    readerKey: EcPrivateKey?,
    readerCert: X509Cert?,
    readerRootCert: X509Cert?
): ByteArray {

    val itemsToRequest = mutableMapOf<String, MutableMap<String, Boolean>>()
    val mdlNs = itemsToRequest.getOrPut(DrivingLicense.MDL_NAMESPACE) { mutableMapOf() }
    when (query) {
        /*ReaderQuery.AGE_OVER_18 -> {
            mdlNs.put("age_over_18", intentToRetain)
            mdlNs.put("portrait", intentToRetain)
        }
        ReaderQuery.AGE_OVER_21 -> {
            mdlNs.put("age_over_21", intentToRetain)
            mdlNs.put("portrait", intentToRetain)
        }
        ReaderQuery.IDENTIFICATION -> {
            mdlNs.put("given_name", intentToRetain)
            mdlNs.put("family_name", intentToRetain)
            mdlNs.put("birth_date", intentToRetain)
            mdlNs.put("birth_place", intentToRetain)
            mdlNs.put("sex", intentToRetain)
            mdlNs.put("portrait", intentToRetain)
            mdlNs.put("resident_address", intentToRetain)
            mdlNs.put("resident_city", intentToRetain)
            mdlNs.put("resident_state", intentToRetain)
            mdlNs.put("resident_postal_code", intentToRetain)
            mdlNs.put("resident_country", intentToRetain)
            mdlNs.put("issuing_authority", intentToRetain)
            mdlNs.put("document_number", intentToRetain)
            mdlNs.put("issue_date", intentToRetain)
            mdlNs.put("expiry_date", intentToRetain)
        }*/

        ReaderQuery.VALID_MEMBERSHIP_CARD -> {
            // Use PhotoID namespace instead of mDL namespace
            val photoIdNs = itemsToRequest.getOrPut(LoyaltyID.LOYALTY_ID_NAMESPACE) { mutableMapOf() }
            photoIdNs.put("membership_number", intentToRetain)
            photoIdNs.put("family_name", intentToRetain)
            photoIdNs.put("given_name", intentToRetain)
            photoIdNs.put("portrait", intentToRetain)
            photoIdNs.put("issue_date", intentToRetain)
            photoIdNs.put("expiry_date", intentToRetain)
            photoIdNs.put("age_over_21", intentToRetain)
        }

//        ReaderQuery.PHOTO_ID -> {
//            // Use PhotoID namespace instead of mDL namespace
//            val photoIdNs = itemsToRequest.getOrPut(PhotoID.PHOTO_ID_NAMESPACE) { mutableMapOf() }
//            // photoIdNs.put("portrait", intentToRetain)
//            photoIdNs.put("person_id", intentToRetain)
//            photoIdNs.put("given_name", intentToRetain)
//            photoIdNs.put("family_name", intentToRetain)
//            photoIdNs.put("birth_date", intentToRetain)
//            photoIdNs.put("birth_place", intentToRetain)
//            photoIdNs.put("sex", intentToRetain)
//            photoIdNs.put("resident_address", intentToRetain)
//            photoIdNs.put("resident_city", intentToRetain)
//            photoIdNs.put("resident_state", intentToRetain)
//            photoIdNs.put("resident_postal_code", intentToRetain)
//            photoIdNs.put("resident_country", intentToRetain)
//            photoIdNs.put("issuing_authority", intentToRetain)
//            photoIdNs.put("document_number", intentToRetain)
//            photoIdNs.put("issue_date", intentToRetain)
//            photoIdNs.put("expiry_date", intentToRetain)
//            photoIdNs.put("age_over_18", intentToRetain)
//            photoIdNs.put("age_over_21", intentToRetain)
//            val photoIdIso232202Ns = itemsToRequest.getOrPut(PhotoID.ISO_23220_2_NAMESPACE) { mutableMapOf() }
//            photoIdIso232202Ns.put("portrait", intentToRetain)
//        }
    }
    // val docType = DrivingLicense.MDL_DOCTYPE
//    val docType = PhotoID.PHOTO_ID_DOCTYPE
    val docType = LoyaltyID.LOYALTY_ID_DOCTYPE

    // TODO: for now we're only requesting an mDL, in the future we might request many different doctypes

    return Cbor.encode(buildDeviceRequest(
        sessionTranscript = RawCbor(encodedSessionTranscript)
    ) {
        if (readerKey != null && readerCert != null && readerRootCert != null) {
            addDocRequest(
                docType = docType,
                nameSpaces = itemsToRequest,
                docRequestInfo = DocRequestInfo(),
                readerKey = readerKey,
                signatureAlgorithm = readerKey.curve.defaultSigningAlgorithm,
                readerKeyCertificateChain = X509CertChain(listOf(readerCert, readerRootCert)),
            )
        } else {
            addDocRequest(
                docType = docType,
                nameSpaces = itemsToRequest,
                docRequestInfo = DocRequestInfo(),
                readerKey = null,
                signatureAlgorithm = Algorithm.UNSET,
                readerKeyCertificateChain = null,
            )
        }
    }.toDataItem())
}


fun List<ReaderQuery>.findIndexForId(id: String): Int? {
    this.forEachIndexed { idx, readerQuery ->
        if (readerQuery.name == id) {
            return idx
        }
    }
    return null
}
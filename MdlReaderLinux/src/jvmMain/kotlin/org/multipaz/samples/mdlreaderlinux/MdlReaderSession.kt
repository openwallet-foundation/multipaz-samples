package org.multipaz.samples.mdlreaderlinux

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.multipaz.bthci.BtHciMdocTransportFactory
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.compose.decodeImage
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodNfc
import org.multipaz.mdoc.engagement.DeviceEngagement
import org.multipaz.mdoc.nfc.MdocHandoverType
import org.multipaz.mdoc.nfc.MdocReaderNfcHandoverOptions
import org.multipaz.mdoc.nfc.scanMdocReader
import org.multipaz.mdoc.request.AlternativeDataElementSet
import org.multipaz.mdoc.request.DeviceRequestInfo
import org.multipaz.mdoc.request.DocRequestInfo
import org.multipaz.mdoc.request.DocumentSet
import org.multipaz.mdoc.request.ElementReference
import org.multipaz.mdoc.request.UseCase
import org.multipaz.mdoc.request.buildDeviceRequest
import org.multipaz.mdoc.response.DeviceResponse
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.sessionencryption.SessionEncryption
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.nfc.NfcTagReaderUsb
import org.multipaz.nfc.create
import org.multipaz.util.Constants
import org.multipaz.util.UUID
import kotlin.time.Clock

data class MdlVerificationResult(
    val portrait: ImageBitmap?,
    val isOlderThan21: Boolean,
    val ageSourceAttribute: String
)

class MdlReaderSession(
    val usbVendorId: Int,
    val usbProductId: Int
) {
    private val nfcReader = NfcTagReaderUsb.create(
        vendorId = usbVendorId,
        productId = usbProductId
    )
    private val bleTransportFactory = BtHciMdocTransportFactory()

    /**
     * Waits for an NFC tap on the external USB NFC reader, performs NFC engagement/handover
     * (supporting both Negotiated Handover and ISO/IEC 18013-5 Second Edition NFC CCE),
     * connects over BLE (via multipaz-bthci) or NFC, requests `portrait` and
     * `(age_over_21 OR age_in_years OR birth_date)`, verifies the response MSO & device auth,
     * and returns the [MdlVerificationResult].
     */
    suspend fun readAndVerifyMdl(
        onStatusChanged: (String) -> Unit
    ): MdlVerificationResult {
        val bleUuid = UUID.randomUUID()
        val negotiatedHandoverConnectionMethods = listOf<MdocConnectionMethod>(
            MdocConnectionMethodBle(
                supportsPeripheralServerMode = false,
                supportsCentralClientMode = true,
                peripheralServerModeUuid = null,
                centralClientModeUuid = bleUuid,
            ),
            MdocConnectionMethodBle(
                supportsPeripheralServerMode = true,
                supportsCentralClientMode = false,
                peripheralServerModeUuid = bleUuid,
                centralClientModeUuid = null,
            ),
            MdocConnectionMethodNfc(
                commandDataFieldMaxLength = 0xffff,
                responseDataFieldMaxLength = 0x10000
            )
        )

        while (true) {
            onStatusChanged("Waiting for phone tap on USB NFC reader...")
            val result = nfcReader.scanMdocReader(
                message = null,
                options = MdocTransportOptions(bleUseL2CAP = true),
                handoverOptions = MdocReaderNfcHandoverOptions(useCce = true),
                transportFactory = bleTransportFactory,
                selectConnectionMethod = { connectionMethods -> connectionMethods.firstOrNull() },
                negotiatedHandoverConnectionMethods = negotiatedHandoverConnectionMethods,
                onHandover = { scanResult ->
                    val transport = scanResult.transport
                    try {
                        val connectionLabel = transport.connectionMethod?.toString() ?: "NFC (CCE)"
                        onStatusChanged("Phone detected! Connecting over $connectionLabel...")

                        val deviceEngagement = DeviceEngagement.fromDataItem(
                            Cbor.decode(scanResult.encodedDeviceEngagement.toByteArray())
                        )
                        val eDeviceKey = deviceEngagement.eDeviceKey
                        val eReaderKey = scanResult.eReaderKey
                            ?.takeIf { it.curve == eDeviceKey.curve }
                            ?: Crypto.createEcPrivateKey(eDeviceKey.curve)

                        val encodedEReaderKey = Cbor.encode(eReaderKey.publicKey.toCoseKey().toDataItem())
                        val sessionTranscript = buildCborArray {
                            add(Tagged(Tagged.ENCODED_CBOR, Bstr(scanResult.encodedDeviceEngagement.toByteArray())))
                            add(Tagged(Tagged.ENCODED_CBOR, Bstr(encodedEReaderKey)))
                            add(scanResult.handover)
                        }

                        val sessionEncryption = SessionEncryption(
                            role = MdocRole.MDOC_READER,
                            eSelfKey = eReaderKey,
                            remotePublicKey = eDeviceKey,
                            encodedSessionTranscript = Cbor.encode(sessionTranscript),
                            insertSequenceNumbers = scanResult.type == MdocHandoverType.CCE_HANDOVER
                        )

                        val deviceRequest = buildDeviceRequest(
                            sessionTranscript = sessionTranscript,
                            deviceRequestInfo = DeviceRequestInfo.fromValues(
                                useCases = listOf(
                                    UseCase(
                                        mandatory = true,
                                        documentSets = listOf(DocumentSet(listOf(0))),
                                        purposeHints = emptyMap()
                                    )
                                )
                            )
                        ) {
                            addDocRequest(
                                docType = DrivingLicense.MDL_DOCTYPE,
                                nameSpaces = mapOf(
                                    DrivingLicense.MDL_NAMESPACE to mapOf(
                                        "portrait" to false,
                                        "age_over_21" to false,
                                    )
                                ),
                                docRequestInfo = DocRequestInfo(
                                    alternativeDataElements = listOf(
                                        AlternativeDataElementSet(
                                            requestedElement = ElementReference(
                                                namespace = DrivingLicense.MDL_NAMESPACE,
                                                dataElement = "age_over_21"
                                            ),
                                            alternativeElementSets = listOf(
                                                listOf(
                                                    ElementReference(
                                                        namespace = DrivingLicense.MDL_NAMESPACE,
                                                        dataElement = "age_in_years"
                                                    )
                                                ),
                                                listOf(
                                                    ElementReference(
                                                        namespace = DrivingLicense.MDL_NAMESPACE,
                                                        dataElement = "birth_date"
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        }

                        transport.open(eDeviceKey)
                        onStatusChanged("Requesting mDL attributes (portrait & age > 21)...")

                        val encryptedDeviceRequest = sessionEncryption.encryptMessage(
                            messagePlaintext = Cbor.encode(deviceRequest.toDataItem()),
                            statusCode = null
                        )
                        transport.sendMessage(encryptedDeviceRequest)

                        val sessionData = transport.waitForMessage()
                        if (sessionData.isEmpty()) {
                            throw IllegalStateException("Holder terminated session without sending a response")
                        }

                        val (responseBytes, status) = sessionEncryption.decryptMessage(sessionData)
                        if (status != Constants.SESSION_DATA_STATUS_SESSION_TERMINATION) {
                            try {
                                transport.sendMessage(
                                    sessionEncryption.encryptMessage(
                                        messagePlaintext = null,
                                        statusCode = Constants.SESSION_DATA_STATUS_SESSION_TERMINATION
                                    )
                                )
                            } catch (_: Throwable) {
                                // Ignore errors when sending session termination
                            }
                        }

                        if (responseBytes == null) {
                            throw IllegalStateException("Empty DeviceResponse payload (status=$status)")
                        }

                        onStatusChanged("Verifying mDL response...")
                        val now = Clock.System.now()
                        val deviceResponse = DeviceResponse.fromDataItem(Cbor.decode(responseBytes))
                        deviceResponse.verify(
                            sessionTranscript = sessionTranscript,
                            eReaderKey = AsymmetricKey.AnonymousExplicit(eReaderKey),
                            atTime = now
                        )

                        val mdlDoc = deviceResponse.documents.firstOrNull {
                            it.docType == DrivingLicense.MDL_DOCTYPE
                        } ?: deviceResponse.documents.firstOrNull()
                            ?: throw IllegalStateException("No mDL document returned in DeviceResponse (status=${deviceResponse.status})")

                        val mdlValues = mdlDoc.issuerNamespaces.data[DrivingLicense.MDL_NAMESPACE]
                            ?: throw IllegalStateException("Missing ${DrivingLicense.MDL_NAMESPACE} namespace in mDL response")

                        val portraitBytes = mdlValues["portrait"]?.dataElementValue?.asBstr
                        val portraitBitmap = portraitBytes?.let { decodeImage(it) }

                        val (isOlderThan21, ageSource) = when {
                            mdlValues["age_over_21"] != null -> {
                                mdlValues["age_over_21"]!!.dataElementValue.asBoolean to "age_over_21"
                            }
                            mdlValues["age_in_years"] != null -> {
                                val ageInYears = mdlValues["age_in_years"]!!.dataElementValue.asNumber
                                (ageInYears >= 21L) to "age_in_years ($ageInYears)"
                            }
                            mdlValues["birth_date"] != null -> {
                                val birthDate = mdlValues["birth_date"]!!.dataElementValue.asDateString
                                val turns21Instant = birthDate.plus(21, DateTimeUnit.YEAR)
                                    .atStartOfDayIn(TimeZone.currentSystemDefault())
                                (now >= turns21Instant) to "birth_date ($birthDate)"
                            }
                            else -> {
                                throw IllegalStateException(
                                    "Response did not include age_over_21, age_in_years, or birth_date"
                                )
                            }
                        }

                        MdlVerificationResult(
                            portrait = portraitBitmap,
                            isOlderThan21 = isOlderThan21,
                            ageSourceAttribute = ageSource
                        )
                    } finally {
                        try {
                            transport.close()
                        } catch (_: Throwable) {
                        }
                    }
                }
            )
            if (result != null) {
                return result
            }
        }
    }
}

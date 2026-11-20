package org.multipaz.samples.isomdocreaderlinux

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.io.bytestring.ByteString
import org.multipaz.asn1.ASN1Integer
import org.multipaz.bthci.BtHciMdocTransportFactory
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.CborMap
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.compose.decodeImage
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.PhotoID
import org.multipaz.mdoc.connectionmethod.BleAddressType
import org.multipaz.mdoc.connectionmethod.BlePeripheralAddress
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocEngagementType
import org.multipaz.mdoc.engagement.DeviceEngagement
import org.multipaz.mdoc.engagement.ReaderEngagement
import org.multipaz.mdoc.engagement.buildReaderEngagement
import org.multipaz.mdoc.nfc.MdocHandoverType
import org.multipaz.mdoc.nfc.MdocReaderNfcEngagementType
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
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.mdoc.transport.MdocTransportStats
import org.multipaz.mdoc.transport.NfcCceTransportMdocReader
import org.multipaz.mdoc.transport.advertise
import org.multipaz.mdoc.transport.waitForConnection
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.nfc.NfcScanOptions
import org.multipaz.nfc.NfcTagReaderUsb
import org.multipaz.nfc.create
import org.multipaz.util.Constants
import org.multipaz.util.Logger
import org.multipaz.util.UUID
import org.multipaz.util.fromHex
import org.multipaz.util.toBase64Url
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

data class IsoMdocVerificationResult(
    val portrait: ImageBitmap?,
    val isOlderThan21: Boolean,
    val ageSourceAttribute: String,
    val transportStats: MdocTransportStats,
    val totalDuration: Duration,
)

data class ConnectedUsbNfcReader(
    val busNumber: Int,
    val deviceAddress: Int,
    val vendorId: Int,
    val productId: Int,
    val productName: String?,
    val deviceNodePath: String,
) {
    val vidPidString: String
        get() = "%04x:%04x".format(vendorId, productId)

    val displayLabel: String
        get() = if (!productName.isNullOrBlank()) {
            "$productName ($vidPidString)"
        } else {
            vidPidString
        }
}

private const val TAG = "IsoMdocReaderSession"
private const val DEFAULT_HCI_L2CAP_PSM = 0x0080
private const val READER_CA_COMMON_NAME = "Multipaz IsoMdocReaderLinux Reader CA (TEST)"
private const val READER_CERT_COMMON_NAME = "Multipaz IsoMdocReaderLinux (TEST)"

private val READER_ROOT_PUBLIC_KEY_PEM = """
    -----BEGIN PUBLIC KEY-----
    MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEJTwvAFf6kSXERSy7ovtEpZ0YxxSd6tZaoAucpa48X39a
    EIFO5V/vUO1npq466R0luHPq7qtkENIcTSFStdGcf2xolHDu2HZcrDxPDXBrMgEfrWqevXDfGT38
    a6s90viw
    -----END PUBLIC KEY-----
""".trimIndent().trim()

private val READER_ROOT_PRIVATE_KEY_PEM = """
    -----BEGIN PRIVATE KEY-----
    MFcCAQAwEAYHKoZIzj0CAQYFK4EEACIEQDA+AgEBBDAFBusGCsPnP5ZXrAzQJqAa0iMm6fUI8RCV
    uZ+7diRJYpdG5k/k9bFASNrgbeYPwOqgBwYFK4EEACI=
    -----END PRIVATE KEY-----
""".trimIndent().trim()

/**
 * Self-signed P-384 Reader CA root certificate (`CN=Multipaz IsoMdocReaderLinux Reader CA (TEST)`),
 * valid for 10 years (`2026-01-01T00:00:00Z` to `2036-01-01T00:00:00Z`).
 */
val READER_ROOT_CERT_PEM = """
    -----BEGIN CERTIFICATE-----
    MIICDjCCAZWgAwIBAgIQNKTYMBT14S4t6rtQ05O0KDAKBggqhkjOPQQDAzA3MTUwMwYDVQQDDCxN
    dWx0aXBheiBJc29NZG9jUmVhZGVyTGludXggUmVhZGVyIENBIChURVNUKTAeFw0yNjAxMDEwMDAw
    MDBaFw0zNjAxMDEwMDAwMDBaMDcxNTAzBgNVBAMMLE11bHRpcGF6IElzb01kb2NSZWFkZXJMaW51
    eCBSZWFkZXIgQ0EgKFRFU1QpMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEJTwvAFf6kSXERSy7ovtE
    pZ0YxxSd6tZaoAucpa48X39aEIFO5V/vUO1npq466R0luHPq7qtkENIcTSFStdGcf2xolHDu2HZc
    rDxPDXBrMgEfrWqevXDfGT38a6s90viwo2YwZDAOBgNVHQ8BAf8EBAMCAQYwEgYDVR0TAQH/BAgw
    BgEB/wIBADAdBgNVHQ4EFgQU/kh1tsOyUHjGQKRgw26KnX9h6WAwHwYDVR0jBBgwFoAU/kh1tsOy
    UHjGQKRgw26KnX9h6WAwCgYIKoZIzj0EAwMDZwAwZAIwDQEyTrRUCmbMdHpUoVtFOUKyv3Aar1zr
    MU64WhMQFUAOPjvtGK7ujInJ8U2hVZBCAjAXyGTe5crpECf2qh8UEaclLB/iftKjoTSQ4X5r8xfm
    xSV8f25XmZCYwKblzqFNgoE=
    -----END CERTIFICATE-----
""".trimIndent().trim()

private val bundledReaderRootKey: AsymmetricKey.X509CertifiedExplicit by lazy {
    val publicKey = EcPublicKey.fromPem(READER_ROOT_PUBLIC_KEY_PEM)
    val privateKey = EcPrivateKey.fromPem(READER_ROOT_PRIVATE_KEY_PEM, publicKey)
    val cert = X509Cert.fromPem(READER_ROOT_CERT_PEM)
    AsymmetricKey.X509CertifiedExplicit(
        certChain = X509CertChain(listOf(cert)),
        privateKey = privateKey,
    )
}

private sealed interface CycleOutcome {
    data class Success(val result: IsoMdocVerificationResult) : CycleOutcome
    data object Restart : CycleOutcome
}

private class DeferredAdvertiseTransport(
    private val underlying: MdocTransport,
    override val connectionMethods: List<MdocConnectionMethod>,
    private val onBeforeOpen: suspend () -> Unit,
) : MdocTransport() {
    override val state: StateFlow<State>
        get() = underlying.state

    override val role: MdocRole
        get() = underlying.role

    override val connectionMethod: MdocConnectionMethod?
        get() = connectionMethods.firstOrNull() ?: underlying.connectionMethod

    override val scanningTime: Duration?
        get() = underlying.scanningTime

    override val stats: MdocTransportStats
        get() = underlying.stats

    override suspend fun advertise() {
        // Deferred until open() so the Reader QR BLE transport can advertise on the HCI controller while waiting.
    }

    override suspend fun open(eSenderKeyBytes: ByteString) {
        onBeforeOpen()
        underlying.open(eSenderKeyBytes)
    }

    override suspend fun sendMessage(message: ByteArray) {
        underlying.sendMessage(message)
    }

    override suspend fun waitForMessage(): ByteArray =
        underlying.waitForMessage()

    override suspend fun close() {
        underlying.close()
    }
}

private fun generateStaticRandomAddress(): Pair<ByteArray, BlePeripheralAddress> {
    val bytes = Crypto.secureRandom.nextBytes(6)
    // Static random address per Bluetooth Core Spec Vol 6 Part B Section 1.3.2.1:
    // Two MSBs of bytes[5] must be 11, and the random part must not be all 0s or all 1s.
    bytes[5] = ((bytes[5].toInt() and 0x3F) or 0xC0).toByte()
    if (bytes[0] == 0.toByte() && bytes[1] == 0.toByte()) {
        bytes[0] = 0x01
    }
    if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFF.toByte()) {
        bytes[0] = 0xFE.toByte()
    }
    val peripheralAddress = BlePeripheralAddress(
        address = ByteString(
            bytes[5],
            bytes[4],
            bytes[3],
            bytes[2],
            bytes[1],
            bytes[0],
        ),
        addressType = BleAddressType.RANDOM,
    )
    return Pair(bytes, peripheralAddress)
}

private fun readSysfsText(dir: File, name: String): String? {
    val file = File(dir, name)
    if (!file.isFile) return null
    return try {
        file.readText().trim().takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}

internal fun findConnectedUsbNfcReader(
    filterVendorId: Int?,
    filterProductId: Int?,
): ConnectedUsbNfcReader? {
    val sysfsRoot = File("/sys/bus/usb/devices")
    val entries = sysfsRoot.listFiles() ?: return null

    // Find all interface directories with bInterfaceClass == 0b (USB CCID)
    val ccidDeviceNames = mutableSetOf<String>()
    for (entry in entries) {
        if (!entry.name.contains(':')) continue
        val iclass = readSysfsText(entry, "bInterfaceClass") ?: continue
        if (iclass.equals("0b", ignoreCase = true)) {
            ccidDeviceNames.add(entry.name.substringBefore(':'))
        }
    }

    for (devName in ccidDeviceNames.sorted()) {
        val devDir = File(sysfsRoot, devName)
        if (!devDir.isDirectory) continue
        val vid = readSysfsText(devDir, "idVendor")?.toIntOrNull(16) ?: continue
        val pid = readSysfsText(devDir, "idProduct")?.toIntOrNull(16) ?: continue
        if (filterVendorId != null && vid != filterVendorId) continue
        if (filterProductId != null && pid != filterProductId) continue

        val busNum = readSysfsText(devDir, "busnum")?.toIntOrNull() ?: continue
        val devNum = readSysfsText(devDir, "devnum")?.toIntOrNull() ?: continue
        val devNodePath = "/dev/bus/usb/%03d/%03d".format(busNum, devNum)
        val devNodeFile = File(devNodePath)
        val accessible = try {
            devNodeFile.exists() &&
                Files.isReadable(devNodeFile.toPath()) &&
                Files.isWritable(devNodeFile.toPath())
        } catch (_: Exception) {
            false
        }
        if (!accessible) continue

        val manufacturer = readSysfsText(devDir, "manufacturer")
        val product = readSysfsText(devDir, "product")
        val niceName = when {
            !manufacturer.isNullOrBlank() && !product.isNullOrBlank() -> {
                if (product.startsWith(manufacturer, ignoreCase = true)) {
                    product
                } else {
                    "$manufacturer $product"
                }
            }
            !product.isNullOrBlank() -> product
            !manufacturer.isNullOrBlank() -> manufacturer
            else -> null
        }

        return ConnectedUsbNfcReader(
            busNumber = busNum,
            deviceAddress = devNum,
            vendorId = vid,
            productId = pid,
            productName = niceName,
            deviceNodePath = devNodePath,
        )
    }
    return null
}

private fun observeConnectedUsbNfcReader(
    filterVendorId: Int?,
    filterProductId: Int?,
    pollInterval: Duration = 300.milliseconds,
): Flow<ConnectedUsbNfcReader?> = flow {
    while (currentCoroutineContext().isActive) {
        val reader = withContext(Dispatchers.IO) {
            findConnectedUsbNfcReader(filterVendorId, filterProductId)
        }
        emit(reader)
        delay(pollInterval)
    }
}.distinctUntilChanged()

class IsoMdocReaderSession(
    val usbVendorId: Int? = null,
    val usbProductId: Int? = null,
) {
    private val sessionMutex = Mutex()

    /**
     * Simultaneously advertises a Reader QR code over BLE (via `multipaz-bthci`) and monitors for
     * an attached USB CCID NFC reader (supporting hot-plug and hot-remove). When a USB NFC reader
     * is present, waits for an NFC tap (supporting both Negotiated Handover and ISO/IEC 18013-5
     * Second Edition NFC CCE) in parallel with Reader QR. Whichever engagement succeeds first is
     * used to request `portrait` and `(age_over_21 OR age_in_years OR birth_date)`, verify the
     * response MSO & device auth, and return the [IsoMdocVerificationResult].
     */
    suspend fun readAndVerifyIsoMdoc(
        useBle: Boolean = true,
        useL2capInEngagement: Boolean = true,
        usePeripheralMacAddressInEngagement: Boolean = true,
        useReaderAuthentication: Boolean = true,
        responseTimeout: Duration = 30.seconds,
        onStatusChanged: (String) -> Unit,
        onQrCodeChanged: (String?) -> Unit = {},
        onUsbReaderChanged: (ConnectedUsbNfcReader?) -> Unit = {},
    ): IsoMdocVerificationResult {
        sessionMutex.withLock {
            val options = MdocTransportOptions(
                bleUseL2CAP = false,
                bleExportPeripheralAddress = usePeripheralMacAddressInEngagement,
                bleUsePeripheralAddress = usePeripheralMacAddressInEngagement,
            )
            val singleUseReaderPrivateKey = if (useReaderAuthentication) {
                Crypto.createEcPrivateKey(EcCurve.P256)
            } else {
                null
            }
            try {
                val certifiedReaderKey = if (singleUseReaderPrivateKey != null) {
                    val now = Clock.System.now()
                    val rootCert = bundledReaderRootKey.certChain.certificates.first()
                    val validFrom = minOf(now - 1.days, rootCert.validityNotBefore)
                    val validUntil = minOf(now + 30.days, rootCert.validityNotAfter)
                    val readerCert = MdocUtil.generateReaderCertificate(
                        readerRootKey = bundledReaderRootKey,
                        readerKey = singleUseReaderPrivateKey.publicKey,
                        subject = X500Name.fromName("CN=$READER_CERT_COMMON_NAME"),
                        dnsName = null,
                        serial = ASN1Integer.fromRandom(numBits = 128),
                        validFrom = validFrom,
                        validUntil = validUntil,
                    )
                    AsymmetricKey.X509CertifiedExplicit(
                        certChain = X509CertChain(
                            listOf(readerCert) + bundledReaderRootKey.certChain.certificates
                        ),
                        privateKey = singleUseReaderPrivateKey,
                    )
                } else {
                    null
                }

        while (true) {
            onQrCodeChanged(null)
            if (useBle) {
                onStatusChanged("Preparing Reader QR...")
            } else {
                onStatusChanged("Waiting for USB NFC reader (BLE disabled)...")
            }

            val (qrRandomAddrBytes, _) = generateStaticRandomAddress()
            val (nfcRandomAddrBytes, nfcPeripheralAddress) = generateStaticRandomAddress()

            val qrBleTransportFactory = BtHciMdocTransportFactory(randomAddress = qrRandomAddrBytes)
            val baseNfcBleTransportFactory = BtHciMdocTransportFactory(randomAddress = nfcRandomAddrBytes)

            val qrBleUuid = UUID.randomUUID()
            val qrConnectionMethods = buildList<MdocConnectionMethod> {
                if (useBle) {
                    if (useL2capInEngagement) {
                        add(MdocConnectionMethod.Ble.DirectL2capCentral(uuid = qrBleUuid))
                    }
                    add(MdocConnectionMethod.Ble.GattCentral(uuid = qrBleUuid))
                }
            }

            val qrEReaderKey = Crypto.createEcPrivateKey(EcCurve.P256)
            var qrAdvertisedTransports: List<MdocTransport> = emptyList()

            try {
                val readerEngagement: ReaderEngagement?
                val qrHandover: DataItem?
                if (useBle) {
                    qrAdvertisedTransports = qrConnectionMethods.advertise(
                        role = MdocRole.MDOC_READER,
                        transportFactory = qrBleTransportFactory,
                        options = options,
                    )
                    val engagement = buildReaderEngagement(
                        eReaderKey = qrEReaderKey.publicKey,
                        version = "1.1",
                    ) {
                        qrAdvertisedTransports.forEach { advertised ->
                            advertised.connectionMethods.forEach { method ->
                                addConnectionMethod(method)
                            }
                        }
                    }
                    val encodedReaderEngagement = Cbor.encode(
                        engagement.toDataItem(MdocEngagementType.READER_QR)
                    )
                    readerEngagement = engagement
                    qrHandover = ReaderEngagement.computeHandover(encodedReaderEngagement)
                    val qrCodeUri = "mdoc:" + encodedReaderEngagement.toBase64Url()
                    onQrCodeChanged(qrCodeUri)
                } else {
                    readerEngagement = null
                    qrHandover = null
                }

                val outcome = coroutineScope {
                    val mutex = Mutex()
                    var qrStoppedForNfc = false
                    val outcomeDeferred = CompletableDeferred<CycleOutcome>()
                    lateinit var nfcJob: Job

                    val qrJob = launch(start = CoroutineStart.LAZY) {
                        if (!useBle || readerEngagement == null || qrHandover == null) {
                            return@launch
                        }
                        var connectedQrTransport: MdocTransport? = null
                        try {
                            val transport = qrAdvertisedTransports.waitForConnection(
                                eSenderKeyBytes = readerEngagement.eReaderKeyBytes!!
                            )
                            val connectionDetectedAt = Clock.System.now()
                            connectedQrTransport = transport
                            onQrCodeChanged(null)

                            val connectionLabel = transport.connectionMethod?.toString() ?: "BLE (Reader QR)"
                            onStatusChanged("QR code scanned! Connecting over $connectionLabel...")

                            val res = withTimeout(responseTimeout) {
                                val initialSessionMessage = transport.waitForMessage()
                                if (initialSessionMessage.isEmpty()) {
                                    throw IllegalStateException(
                                        "Holder terminated Reader QR session without sending DeviceEngagement"
                                    )
                                }
                                val encodedDeBytes = SessionEncryption.getDeviceEngagementBytes(initialSessionMessage)
                                val deviceEngagement = DeviceEngagement.fromDataItem(
                                    dataItem = Cbor.decode(encodedDeBytes),
                                    engagementType = MdocEngagementType.READER_QR,
                                )
                                val eDeviceKey = deviceEngagement.eDeviceKey
                                val matchingQrKey = qrEReaderKey.takeIf { it.curve == eDeviceKey.curve }
                                val effectiveEReaderKey = matchingQrKey ?: Crypto.createEcPrivateKey(eDeviceKey.curve)

                                exchangeAndVerifyIsoMdoc(
                                    transport = transport,
                                    encodedDeviceEngagement = encodedDeBytes,
                                    deviceEngagement = deviceEngagement,
                                    eReaderKey = effectiveEReaderKey,
                                    handover = qrHandover,
                                    insertSequenceNumbers = false,
                                    alreadyOpened = true,
                                    sendSessionEstablishment = (matchingQrKey == null),
                                    certifiedReaderKey = certifiedReaderKey,
                                    initialDuration = Duration.ZERO,
                                    transferStartedAt = connectionDetectedAt,
                                    onStatusChanged = onStatusChanged,
                                )
                            }
                            if (outcomeDeferred.complete(CycleOutcome.Success(res))) {
                                nfcJob.cancel()
                            }
                        } catch (e: TimeoutCancellationException) {
                            Logger.i(TAG, "Timed out waiting for response over Reader QR; restarting transaction", e)
                            if (outcomeDeferred.complete(CycleOutcome.Restart)) {
                                nfcJob.cancel()
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            if (outcomeDeferred.completeExceptionally(e)) {
                                nfcJob.cancel()
                            }
                        } finally {
                            withContext(NonCancellable) {
                                try {
                                    connectedQrTransport?.close()
                                } catch (_: Throwable) {
                                }
                            }
                        }
                    }

                    suspend fun stopQrForNfc() {
                        mutex.withLock {
                            if (qrStoppedForNfc) return
                            qrStoppedForNfc = true
                        }
                        onQrCodeChanged(null)
                        withContext(NonCancellable) {
                            qrJob.cancelAndJoin()
                            for (t in qrAdvertisedTransports) {
                                try {
                                    t.close()
                                } catch (_: Throwable) {
                                }
                            }
                        }
                    }

                    val nfcBleUuid = UUID.randomUUID()
                    val nfcNegotiatedHandoverConnectionMethods = buildList<MdocConnectionMethod> {
                        if (useBle) {
                            if (useL2capInEngagement) {
                                add(MdocConnectionMethod.Ble.DirectL2capCentral(uuid = nfcBleUuid))
                            }
                            add(MdocConnectionMethod.Ble.GattCentral(uuid = nfcBleUuid))
                            if (useL2capInEngagement) {
                                add(MdocConnectionMethod.Ble.DirectL2capPeripheral())
                            }
                            add(MdocConnectionMethod.Ble.GattPeripheral())
                        }
                    }

                    val nfcTransportFactory = object : MdocTransportFactory {
                        override fun createTransport(
                            connectionMethod: MdocConnectionMethod,
                            role: MdocRole,
                            options: MdocTransportOptions,
                        ): MdocTransport {
                            val transport = baseNfcBleTransportFactory.createTransport(connectionMethod, role, options)
                            return if (connectionMethod is MdocConnectionMethod.Ble) {
                                DeferredAdvertiseTransport(
                                    underlying = transport,
                                    connectionMethods = listOf(connectionMethod),
                                    onBeforeOpen = ::stopQrForNfc,
                                )
                            } else {
                                transport
                            }
                        }

                        override fun createTransports(
                            connectionMethods: List<MdocConnectionMethod>,
                            role: MdocRole,
                            options: MdocTransportOptions,
                        ): List<MdocTransport> {
                            val result = mutableListOf<MdocTransport>()
                            val centralBleMethods = connectionMethods.filter {
                                it is MdocConnectionMethod.Ble && it.isMdocCentral
                            }
                            if (centralBleMethods.isNotEmpty()) {
                                val exportedAddress = if (options.bleExportPeripheralAddress) {
                                    nfcPeripheralAddress
                                } else {
                                    null
                                }
                                val methodsWithAddr = centralBleMethods.map { cm ->
                                    when (cm) {
                                        is MdocConnectionMethod.Ble.DirectL2capCentral ->
                                            cm.copy(peripheralAddress = exportedAddress)
                                        is MdocConnectionMethod.Ble.GattCentral ->
                                            cm.copy(peripheralAddress = exportedAddress)
                                        else -> cm
                                    }
                                }
                                val underlying = baseNfcBleTransportFactory.createTransports(
                                    connectionMethods = methodsWithAddr,
                                    role = role,
                                    options = options,
                                ).single()
                                val advertisedMethods = methodsWithAddr.map { cm ->
                                    when (cm) {
                                        is MdocConnectionMethod.Ble.DirectL2capCentral ->
                                            cm.copy(
                                                psm = DEFAULT_HCI_L2CAP_PSM,
                                                peripheralAddress = exportedAddress,
                                            )
                                        is MdocConnectionMethod.Ble.GattCentral ->
                                            cm.copy(peripheralAddress = exportedAddress)
                                        else -> cm
                                    }
                                }
                                result.add(
                                    DeferredAdvertiseTransport(
                                        underlying = underlying,
                                        connectionMethods = advertisedMethods,
                                        onBeforeOpen = ::stopQrForNfc,
                                    )
                                )
                            }
                            for (cm in connectionMethods) {
                                if (cm is MdocConnectionMethod.Ble && cm.isMdocCentral) continue
                                result.add(createTransport(cm, role, options))
                            }
                            return result
                        }
                    }

                    nfcJob = launch(start = CoroutineStart.LAZY) {
                        observeConnectedUsbNfcReader(usbVendorId, usbProductId).collectLatest { readerInfo ->
                            onUsbReaderChanged(readerInfo)
                            if (readerInfo == null) {
                                val wasTransactingNfc = mutex.withLock { qrStoppedForNfc }
                                if (wasTransactingNfc) {
                                    // USB NFC reader was unplugged after QR had already stopped for NFC;
                                    // restart the cycle so Reader QR is re-advertised.
                                    if (outcomeDeferred.complete(CycleOutcome.Restart)) {
                                        qrJob.cancel()
                                    }
                                } else if (useBle) {
                                    onStatusChanged("Waiting for QR scan (no USB NFC reader connected)...")
                                } else {
                                    onStatusChanged("Waiting for USB NFC reader (BLE disabled)...")
                                }
                                return@collectLatest
                            }

                            val wasTransactingNfc = mutex.withLock { qrStoppedForNfc }
                            if (!wasTransactingNfc) {
                                if (useBle) {
                                    onStatusChanged("Waiting for QR scan or phone tap on USB NFC reader...")
                                } else {
                                    onStatusChanged("Waiting for phone tap on USB NFC reader...")
                                }
                            }

                            val nfcReader = NfcTagReaderUsb.create(devicePath = readerInfo.deviceNodePath)
                            try {
                                // When BLE is disabled, no alternative data transfer connection methods are
                                // offered (MdocConnectionMethod.Nfc is intentionally omitted because 2021-edition
                                // NFC data transfer requires keeping the phone in the field continuously, which
                                // doesn't work when the user must remove the phone to perform biometric unlock).
                                // Consequently, NDEF Connection Handover cannot succeed without BLE, so we must
                                // restrict NFC engagement to Second Edition NFC CCE (NFC_CCE_ONLY), which
                                // supports re-tapping after biometric authentication.
                                val nfcEngagementType = if (useBle) {
                                    MdocReaderNfcEngagementType.NFC_CCE_AND_NFC_CONNECTION_HANDOVER
                                } else {
                                    MdocReaderNfcEngagementType.NFC_CCE_ONLY
                                }
                                val res = nfcReader.scanMdocReader(
                                    message = null,
                                    options = options,
                                    handoverOptions = MdocReaderNfcHandoverOptions(
                                        engagementType = nfcEngagementType
                                    ),
                                    transportFactory = nfcTransportFactory,
                                    selectConnectionMethod = { connectionMethods ->
                                        if (useBle) {
                                            connectionMethods.firstOrNull { it is MdocConnectionMethod.Ble }
                                        } else {
                                            null
                                        }
                                    },
                                    negotiatedHandoverConnectionMethods = nfcNegotiatedHandoverConnectionMethods,
                                    nfcScanOptions = NfcScanOptions(
                                        pollingFrameData = ByteString("6a0281030000".fromHex())
                                    ),
                                    onHandover = { scanResult ->
                                        val handoverCompletedAt = Clock.System.now()
                                        stopQrForNfc()
                                        val transport = scanResult.transport
                                        val isNfcOnly =
                                            (transport as? NfcCceTransportMdocReader)?.isNfcOnly == true
                                        try {
                                            val connectionLabel = transport.connectionMethod?.toString() ?: "NFC (CCE)"
                                            onStatusChanged("Phone detected via NFC! Connecting over $connectionLabel...")

                                            val encodedDeBytes = scanResult.encodedDeviceEngagement.toByteArray()
                                            val deviceEngagement = DeviceEngagement.fromDataItem(
                                                Cbor.decode(encodedDeBytes)
                                            )
                                            val eDeviceKey = deviceEngagement.eDeviceKey
                                            val eReaderKey = scanResult.eReaderKey
                                                ?.takeIf { it.curve == eDeviceKey.curve }
                                                ?: Crypto.createEcPrivateKey(eDeviceKey.curve)

                                            withTimeout(responseTimeout) {
                                                exchangeAndVerifyIsoMdoc(
                                                    transport = transport,
                                                    encodedDeviceEngagement = encodedDeBytes,
                                                    deviceEngagement = deviceEngagement,
                                                    eReaderKey = eReaderKey,
                                                    handover = scanResult.handover,
                                                    insertSequenceNumbers = scanResult.type == MdocHandoverType.CCE_HANDOVER,
                                                    alreadyOpened = false,
                                                    sendSessionEstablishment = true,
                                                    certifiedReaderKey = certifiedReaderKey,
                                                    initialDuration = scanResult.processingDuration,
                                                    transferStartedAt = handoverCompletedAt,
                                                    onStatusChanged = onStatusChanged,
                                                )
                                            }
                                        } catch (e: TimeoutCancellationException) {
                                            Logger.i(
                                                TAG,
                                                "Timed out waiting for connection or response after NFC tap; restarting transaction",
                                                e
                                            )
                                            if (outcomeDeferred.complete(CycleOutcome.Restart)) {
                                                qrJob.cancel()
                                            }
                                            null
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Throwable) {
                                            if (isNfcOnly) {
                                                // In NFC-only mode, removing the mdoc from the reader field
                                                // (e.g. so the holder can perform biometric unlock in the
                                                // NFC-only double-tap scenario) terminates the active NFC tag
                                                // link. Abort and set up a new transaction immediately so the
                                                // UI leaves the "Waiting for response" screen and is ready for
                                                // the second tap.
                                                Logger.i(
                                                    TAG,
                                                    "mdoc removed during NFC-only transaction; aborting and starting new transaction",
                                                    e
                                                )
                                                if (outcomeDeferred.complete(CycleOutcome.Restart)) {
                                                    qrJob.cancel()
                                                }
                                                null
                                            } else {
                                                throw e
                                            }
                                        } finally {
                                            withContext(NonCancellable) {
                                                try {
                                                    transport.close()
                                                } catch (_: Throwable) {
                                                }
                                            }
                                        }
                                    }
                                )
                                if (res != null) {
                                    if (outcomeDeferred.complete(CycleOutcome.Success(res))) {
                                        qrJob.cancel()
                                    }
                                } else if (mutex.withLock { qrStoppedForNfc }) {
                                    if (outcomeDeferred.complete(CycleOutcome.Restart)) {
                                        qrJob.cancel()
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                val hadStartedNfcHandover = mutex.withLock { qrStoppedForNfc }
                                val stillConnected = withContext(Dispatchers.IO) {
                                    findConnectedUsbNfcReader(usbVendorId, usbProductId) != null
                                }
                                if (!hadStartedNfcHandover && (!stillConnected || e is IOException)) {
                                    // Reader was hot-removed before any NFC handover started; keep QR running.
                                    onUsbReaderChanged(null)
                                    if (useBle) {
                                        onStatusChanged("Waiting for QR scan (no USB NFC reader connected)...")
                                    } else {
                                        onStatusChanged("Waiting for USB NFC reader (BLE disabled)...")
                                    }
                                } else {
                                    if (outcomeDeferred.completeExceptionally(e)) {
                                        qrJob.cancel()
                                    }
                                }
                            } finally {
                                withContext(NonCancellable) {
                                    try {
                                        nfcReader.close()
                                    } catch (_: Throwable) {
                                    }
                                }
                            }
                        }
                    }

                    if (useBle) {
                        qrJob.start()
                    }
                    nfcJob.start()

                    try {
                        outcomeDeferred.await()
                    } finally {
                        withContext(NonCancellable) {
                            qrJob.cancel()
                            nfcJob.cancel()
                        }
                    }
                }

                if (outcome is CycleOutcome.Success) {
                    return outcome.result
                }
            } finally {
                withContext(NonCancellable) {
                    qrEReaderKey.close()
                    for (t in qrAdvertisedTransports) {
                        try {
                            t.close()
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        }
            } finally {
                singleUseReaderPrivateKey?.close()
            }
        }
    }

    private suspend fun exchangeAndVerifyIsoMdoc(
        transport: MdocTransport,
        encodedDeviceEngagement: ByteArray,
        deviceEngagement: DeviceEngagement,
        eReaderKey: EcPrivateKey,
        handover: DataItem,
        insertSequenceNumbers: Boolean,
        alreadyOpened: Boolean,
        sendSessionEstablishment: Boolean,
        certifiedReaderKey: AsymmetricKey.X509CertifiedExplicit?,
        initialDuration: Duration,
        transferStartedAt: Instant,
        onStatusChanged: (String) -> Unit,
    ): IsoMdocVerificationResult {
        Logger.dCbor(TAG, "DeviceEngagement", encodedDeviceEngagement)
        val eDeviceKey = deviceEngagement.eDeviceKey
        val encodedEReaderKey = Cbor.encode(eReaderKey.publicKey.toCoseKey().toDataItem())
        val sessionTranscript = buildCborArray {
            add(Tagged(Tagged.ENCODED_CBOR, Bstr(encodedDeviceEngagement)))
            add(Tagged(Tagged.ENCODED_CBOR, Bstr(encodedEReaderKey)))
            add(handover)
        }

        val sessionEncryption = SessionEncryption(
            role = MdocRole.MDOC_READER,
            eSelfKey = eReaderKey,
            remotePublicKey = eDeviceKey,
            encodedSessionTranscript = Cbor.encode(sessionTranscript),
            insertSequenceNumbers = insertSequenceNumbers
        )
        sessionEncryption.setSendSessionEstablishment(sendSessionEstablishment)

        val deviceRequest = buildDeviceRequest(
            sessionTranscript = sessionTranscript,
            deviceRequestInfo = DeviceRequestInfo.fromValues(
                useCases = listOf(
                    UseCase(
                        mandatory = true,
                        documentSets = listOf(
                            DocumentSet(listOf(0)),
                            DocumentSet(listOf(1)),
                        ),
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
            addDocRequest(
                docType = PhotoID.PHOTO_ID_DOCTYPE,
                nameSpaces = mapOf(
                    PhotoID.ISO_23220_2_NAMESPACE to mapOf(
                        "portrait" to false,
                        "age_over_21" to false,
                    )
                ),
                docRequestInfo = DocRequestInfo(
                    alternativeDataElements = listOf(
                        AlternativeDataElementSet(
                            requestedElement = ElementReference(
                                namespace = PhotoID.ISO_23220_2_NAMESPACE,
                                dataElement = "age_over_21"
                            ),
                            alternativeElementSets = listOf(
                                listOf(
                                    ElementReference(
                                        namespace = PhotoID.ISO_23220_2_NAMESPACE,
                                        dataElement = "age_in_years"
                                    )
                                ),
                                listOf(
                                    ElementReference(
                                        namespace = PhotoID.ISO_23220_2_NAMESPACE,
                                        dataElement = "birth_date"
                                    )
                                )
                            )
                        )
                    )
                )
            )
            if (certifiedReaderKey != null) {
                addReaderAuthAll(readerKey = certifiedReaderKey)
            }
        }

        if (!alreadyOpened) {
            transport.open(deviceEngagement.eDeviceKeyBytes)
        }
        onStatusChanged("Requesting attributes (portrait & age > 21)...")

        val encodedDeviceRequest = Cbor.encode(deviceRequest.toDataItem())
        Logger.dCbor(TAG, "DeviceRequest", encodedDeviceRequest)
        val encryptedDeviceRequest = sessionEncryption.encryptMessage(
            messagePlaintext = encodedDeviceRequest,
            statusCode = null
        )
        transport.sendMessage(encryptedDeviceRequest)

        val sessionData = transport.waitForMessage()
        val responseReceivedAt = Clock.System.now()
        val totalDuration = initialDuration + (responseReceivedAt - transferStartedAt)
        val transportStats = transport.stats
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
        Logger.dCbor(TAG, "DeviceResponse", responseBytes)

        onStatusChanged("Verifying response...")
        val now = Clock.System.now()
        val deviceResponse = DeviceResponse.fromDataItem(Cbor.decode(responseBytes))
        deviceResponse.verify(
            sessionTranscript = sessionTranscript,
            eReaderKey = AsymmetricKey.AnonymousExplicit(eReaderKey),
            atTime = now
        )

        val returnedDoc = deviceResponse.documents.firstOrNull {
            it.docType == DrivingLicense.MDL_DOCTYPE || it.docType == PhotoID.PHOTO_ID_DOCTYPE
        } ?: deviceResponse.documents.firstOrNull()
            ?: throw IllegalStateException("No mDL or PhotoID document returned in DeviceResponse (status=${deviceResponse.status})")

        val values = returnedDoc.issuerNamespaces.data[DrivingLicense.MDL_NAMESPACE]
            ?: returnedDoc.issuerNamespaces.data[PhotoID.ISO_23220_2_NAMESPACE]
            ?: returnedDoc.issuerNamespaces.data.values.firstOrNull()
            ?: throw IllegalStateException("Missing issuer namespaces in ${returnedDoc.docType} response")

        val portraitBytes = values["portrait"]?.dataElementValue?.asBstr
        val portraitBitmap = portraitBytes?.let { decodeImage(it) }

        val (isOlderThan21, ageSource) = when {
            values["age_over_21"] != null -> {
                values["age_over_21"]!!.dataElementValue.asBoolean to "age_over_21"
            }
            values["age_in_years"] != null -> {
                val ageInYears = values["age_in_years"]!!.dataElementValue.asNumber
                (ageInYears >= 21L) to "age_in_years ($ageInYears)"
            }
            values["birth_date"] != null -> {
                val birthDateItem = values["birth_date"]!!.dataElementValue
                val birthDate = if (birthDateItem is CborMap && birthDateItem.hasKey("birth_date")) {
                    birthDateItem["birth_date"].asDateString
                } else {
                    birthDateItem.asDateString
                }
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

        return IsoMdocVerificationResult(
            portrait = portraitBitmap,
            isOlderThan21 = isOlderThan21,
            ageSourceAttribute = ageSource,
            transportStats = transportStats,
            totalDuration = totalDuration,
        )
    }
}

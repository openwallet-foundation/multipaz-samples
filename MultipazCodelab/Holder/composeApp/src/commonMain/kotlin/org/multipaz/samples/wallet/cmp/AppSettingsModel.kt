package org.multipaz.samples.wallet.cmp

import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Tstr
import org.multipaz.cbor.toDataItem
import org.multipaz.crypto.EcCurve
import org.multipaz.storage.Storage
import org.multipaz.storage.StorageTable
import org.multipaz.storage.StorageTableSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.buildCborArray
import org.multipaz.models.digitalcredentials.DigitalCredentials
import kotlin.Boolean
import kotlin.time.ExperimentalTime

class AppSettingsModel private constructor(
    private val readOnly: Boolean
) {

    private lateinit var settingsTable: StorageTable

    companion object Companion {


        // Default to our open CSA, where "open" means it'll work with even unlocked bootloaders
        // and any application signing key.
        private const val CSA_URL_DEFAULT: String = "https://csa.multipaz.org/open"
        private val tableSpec = StorageTableSpec(
            name = "TestAppSettings",
            supportPartitions = false,
            supportExpiration = false
        )

        /**
         * Asynchronous construction.
         *
         * @param storage the [Storage] backing the settings.
         * @param readOnly if `false`, won't monitor all the settings and write to storage when they change.
         */
        suspend fun create(
            storage: Storage,
            readOnly: Boolean = false
        ): AppSettingsModel {
            val instance = AppSettingsModel(readOnly)
            instance.settingsTable = storage.getTable(tableSpec)
            instance.init()
            return instance
        }
    }

    private data class BoundItem<T>(
        val variable: MutableStateFlow<T>,
        val defaultValue: T
    )

    private val boundItems = mutableListOf<BoundItem<*>>()

    @OptIn(ExperimentalTime::class)
    private suspend inline fun <reified T> bind(
        variable: MutableStateFlow<T>,
        key: String,
        defaultValue: T
    ) {
        val value = settingsTable.get(key)?.let {
            val dataItem = Cbor.decode(it.toByteArray())
            when (T::class) {
                Boolean::class -> {
                    dataItem.asBoolean as T
                }

                String::class -> {
                    dataItem.asTstr as T
                }

                List::class -> {
                    dataItem.asArray.map { item -> (item as Tstr).value } as T
                }

                Set::class -> {
                    dataItem.asArray.map { item -> (item as Tstr).value }.toSet() as T
                }

                EcCurve::class -> {
                    EcCurve.entries.find { curve -> curve.name == dataItem.asTstr } as T
                }

                else -> {
                    throw IllegalStateException("Type not supported")
                }
            }
        } ?: defaultValue
        variable.value = value

        if (!readOnly) {
            CoroutineScope(Dispatchers.Default).launch {
                variable.asStateFlow().collect { newValue ->
                    val dataItem = when (T::class) {
                        Boolean::class -> {
                            (newValue as Boolean).toDataItem()
                        }

                        String::class -> {
                            (newValue as String).toDataItem()
                        }

                        List::class -> {
                            buildCborArray {
                                (newValue as List<*>).forEach { add(Tstr(it as String)) }
                            }
                        }

                        Set::class -> {
                            buildCborArray {
                                (newValue as Set<*>).forEach { add(Tstr(it as String)) }
                            }
                        }

                        EcCurve::class -> {
                            (newValue as EcCurve).name.toDataItem()
                        }

                        else -> {
                            throw IllegalStateException("Type not supported")
                        }
                    }
                    if (settingsTable.get(key) == null) {
                        settingsTable.insert(key, ByteString(Cbor.encode(dataItem)))
                    } else {
                        settingsTable.update(key, ByteString(Cbor.encode(dataItem)))
                    }
                }
            }
        }
        boundItems.add(BoundItem(variable, defaultValue))
    }


    // TODO: use something like KSP to avoid having to repeat settings name three times..
    //

    private suspend fun init() {
        bind(
            presentmentBleCentralClientModeEnabled,
            "presentmentBleCentralClientModeEnabled",
            false
        )
        bind(
            presentmentBlePeripheralServerModeEnabled,
            "presentmentBlePeripheralServerModeEnabled",
            true
        )
        bind(presentmentNfcDataTransferEnabled, "presentmentNfcDataTransferEnabled", false)
        bind(presentmentSessionEncryptionCurve, "presentmentSessionEncryptionCurve", EcCurve.P256)
        bind(presentmentBleL2CapEnabled, "presentmentBleL2CapEnabled", false)
        bind(presentmentBleL2CapInEngagementEnabled, "presentmentBleL2CapInEngagementEnabled", true)
        bind(presentmentUseNegotiatedHandover, "presentmentUseNegotiatedHandover", true)
        bind(presentmentAllowMultipleRequests, "presentmentAllowMultipleRequests", false)
        bind(
            presentmentNegotiatedHandoverPreferredOrder,
            "presentmentNegotiatedHandoverPreferredOrder",
            listOf(
                "ble:central_client_mode:",
                "ble:peripheral_server_mode:",
                "nfc:"
            )
        )
        bind(presentmentShowConsentPrompt, "presentmentShowConsentPrompt", true)
        bind(presentmentRequireAuthentication, "presentmentRequireAuthentication", true)
        bind(
            presentmentPreferSignatureToKeyAgreement,
            "presentmentPreferSignatureToKeyAgreement",
            false
        )

        bind(readerBleCentralClientModeEnabled, "readerBleCentralClientModeEnabled", true)
        bind(readerBlePeripheralServerModeEnabled, "readerBlePeripheralServerModeEnabled", true)
        bind(readerNfcDataTransferEnabled, "readerNfcDataTransferEnabled", true)
        bind(readerBleL2CapEnabled, "readerBleL2CapEnabled", false)
        bind(readerBleL2CapInEngagementEnabled, "readerBleL2CapInEngagementEnabled", true)
        bind(readerAutomaticallySelectTransport, "readerAutomaticallySelectTransport", false)
        bind(readerAllowMultipleRequests, "readerAllowMultipleRequests", false)

        bind(cloudSecureAreaUrl, "cloudSecureAreaUrl", CSA_URL_DEFAULT)
        bind(dcApiProtocols, "dcApiProtocols", DigitalCredentials.Default.supportedProtocols)

        bind(cryptoPreferBouncyCastle, "cryptoForceBouncyCastle", false)
    }

    val presentmentBleCentralClientModeEnabled = MutableStateFlow(false)
    val presentmentBlePeripheralServerModeEnabled = MutableStateFlow(false)
    val presentmentNfcDataTransferEnabled = MutableStateFlow(false)
    val presentmentSessionEncryptionCurve = MutableStateFlow(EcCurve.P256)
    val presentmentBleL2CapEnabled = MutableStateFlow(false)
    val presentmentBleL2CapInEngagementEnabled = MutableStateFlow(false)
    val presentmentUseNegotiatedHandover = MutableStateFlow(false)
    val presentmentAllowMultipleRequests = MutableStateFlow(false)
    val presentmentNegotiatedHandoverPreferredOrder = MutableStateFlow<List<String>>(listOf())
    val presentmentShowConsentPrompt = MutableStateFlow(false)
    val presentmentRequireAuthentication = MutableStateFlow(false)
    val presentmentPreferSignatureToKeyAgreement = MutableStateFlow(false)

    val readerBleCentralClientModeEnabled = MutableStateFlow(false)
    val readerBlePeripheralServerModeEnabled = MutableStateFlow(false)
    val readerNfcDataTransferEnabled = MutableStateFlow(false)
    val readerBleL2CapEnabled = MutableStateFlow(false)
    val readerBleL2CapInEngagementEnabled = MutableStateFlow(false)
    val readerAutomaticallySelectTransport = MutableStateFlow(false)
    val readerAllowMultipleRequests = MutableStateFlow(false)

    val cloudSecureAreaUrl = MutableStateFlow(CSA_URL_DEFAULT)
    val dcApiProtocols = MutableStateFlow(DigitalCredentials.Default.supportedProtocols)

    val cryptoPreferBouncyCastle = MutableStateFlow(false)
}


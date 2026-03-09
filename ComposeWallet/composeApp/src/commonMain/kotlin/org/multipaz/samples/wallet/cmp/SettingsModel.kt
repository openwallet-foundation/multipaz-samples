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
import kotlin.Boolean

/**
 * A model for settings.
 */
class SettingsModel private constructor(
    private val readOnly: Boolean
) {

    private lateinit var settingsTable: StorageTable

    companion object {
        private val tableSpec = StorageTableSpec(
            name = "Settings",
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
        ): SettingsModel {
            val instance = SettingsModel(readOnly)
            instance.settingsTable = storage.getTable(tableSpec)
            instance.init()
            return instance
        }
    }

    private data class BoundItem<T>(
        val variable: MutableStateFlow<T>,
        val defaultValue: T
    ) {
        fun resetValue() {
            variable.value = defaultValue
        }
    }

    private val boundItems = mutableListOf<BoundItem<*>>()

    private suspend inline fun<reified T> bind(
        variable: MutableStateFlow<T>,
        key: String,
        defaultValue: T
    ) {
        val value = settingsTable.get(key)?.let {
            val dataItem = Cbor.decode(it.toByteArray())
            when (T::class) {
                Boolean::class -> { dataItem.asBoolean as T }
                String::class -> { dataItem.asTstr as T }
                List::class -> { dataItem.asArray.map { item -> (item as Tstr).value } as T }
                Set::class -> { dataItem.asArray.map { item -> (item as Tstr).value }.toSet() as T }
                EcCurve::class -> { EcCurve.entries.find { it.name == dataItem.asTstr } as T }
                else -> { throw IllegalStateException("Type not supported") }
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

    fun resetSettings() {
        boundItems.forEach { it.resetValue() }
    }

    // TODO: use something like KSP to avoid having to repeat settings name three times..
    //

    private suspend fun init() {
        bind(currentlyFocusedDocumentId, "currentlyFocusedDocumentId", "")
    }

    val currentlyFocusedDocumentId = MutableStateFlow("")
}

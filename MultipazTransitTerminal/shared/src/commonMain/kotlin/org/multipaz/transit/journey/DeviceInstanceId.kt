package org.multipaz.transit.journey

import kotlinx.io.bytestring.ByteString
import org.multipaz.storage.KeyExistsStorageException
import org.multipaz.storage.StorageTableSpec
import org.multipaz.util.Platform
import org.multipaz.util.toBase64Url
import kotlin.random.Random

/**
 * Stable, random identifier for this installation of the terminal app.
 *
 * It is deliberately kept in Multipaz's app-local persistent storage rather than derived from
 * hardware identifiers. Clearing app data or reinstalling creates a new installation identity.
 */
object DeviceInstanceId {
    private const val RECORD_KEY = "instance"

    private val tableSpec = StorageTableSpec(
        name = "TransitDeviceInstance",
        supportPartitions = false,
        supportExpiration = false,
        schemaVersion = 1,
    )

    suspend fun get(): String {
        val table = Platform.storage.getTable(tableSpec)
        table.get(RECORD_KEY)?.let { return it.toByteArray().decodeToString() }

        val generatedId = Random.Default.nextBytes(16).toBase64Url()
        try {
            table.insert(RECORD_KEY, ByteString(generatedId.encodeToByteArray()))
            return generatedId
        } catch (_: KeyExistsStorageException) {
            // Another app component created the identifier between our read and insert.
            return requireNotNull(table.get(RECORD_KEY)) {
                "Device instance identifier was not persisted"
            }.toByteArray().decodeToString()
        }
    }
}

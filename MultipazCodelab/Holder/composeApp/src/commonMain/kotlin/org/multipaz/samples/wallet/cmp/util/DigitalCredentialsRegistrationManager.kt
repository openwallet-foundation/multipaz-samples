package org.multipaz.samples.wallet.cmp.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.digitalcredentials.getDefault
import org.multipaz.document.DocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.util.Logger

private const val TAG = "DigitalCredentialsReg"

/**
 * Centralizes Android-side W3C DC API registration so it can be refreshed after issuance
 * without depending on PresentmentSource re-instantiation.
 */
class DigitalCredentialsRegistrationManager(
    private val documentStore: DocumentStore,
    private val documentTypeRepository: DocumentTypeRepository,
    private val settingsModel: AppSettingsModel,
) {
    private val registerMutex = Mutex()

    suspend fun refresh(reason: String) {
        if (!shouldRegisterDigitalCredentialsInCommonModule()) {
            return
        }

        registerMutex.withLock {
            val digitalCredentials: DigitalCredentials = DigitalCredentials.getDefault()
            if (!digitalCredentials.registerAvailable) {
                Logger.i(TAG, "Skipping register: API not available ($reason)")
                return
            }

            try {
                // TODO: Register DC API with documents from document store
                digitalCredentials.register(
                    documentStore = documentStore,
                    documentTypeRepository = documentTypeRepository,
                    selectedProtocols = settingsModel.dcApiProtocols.value,
                )
                Logger.i(
                    TAG,
                    "Digital credentials registration refreshed ($reason)",
                )
            } catch (t: Throwable) {
                Logger.w(TAG, "Digital credentials registration refresh failed ($reason)", t)
            }
        }
    }
}

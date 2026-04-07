package org.multipaz.getstarted.core

import org.multipaz.document.Document
import org.multipaz.document.DocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.storage.Storage
import org.multipaz.storage.StorageTable
import org.multipaz.trustmanagement.TrustManagerLocal

interface AppContainer {

    val storage: Storage
    val storageTable: StorageTable
    val secureArea: SecureArea
    val secureAreaRepository: SecureAreaRepository

    val documentTypeRepository: DocumentTypeRepository
    val documentStore: DocumentStore

    val presentmentSource: PresentmentSource

    val readerTrustManager: TrustManagerLocal

    val isInitialized: Boolean

    suspend fun init()
    suspend fun listDocuments(): MutableList<Document>

    companion object {
        val promptModel: PromptModel = org.multipaz.util.Platform.promptModel

        private var instance: AppContainer? = null
        fun getInstance(): AppContainer {
            if (instance == null) {
                instance = AppContainerImpl()
            }
            return instance!!
        }
    }
}

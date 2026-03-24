package org.multipaz.samples.wallet.cmp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.digitalcredentials.getDefault
import org.multipaz.document.DocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.presentment.model.digitalCredentialsPresentment
import org.multipaz.samples.wallet.cmp.di.initKoin
import org.multipaz.samples.wallet.cmp.util.AppSettingsModel
import org.multipaz.util.Logger

private const val TAG = "IosDocumentProvider"

private object IosDocumentProviderComponent : KoinComponent

private var koinInitialized = false
private var registrationStarted = false

@Suppress("FunctionName")
fun EnsureIosDocumentProviderInitialized() {
    if (!koinInitialized) {
        initKoin()
        koinInitialized = true
    }
}

@Suppress("FunctionName")
suspend fun GetIosDocumentProviderPresentmentSource(): PresentmentSource {
    EnsureIosDocumentProviderInitialized()
    return IosDocumentProviderComponent.get()
}

@Suppress("FunctionName")
fun StartIosDigitalCredentialsRegistration() {
    EnsureIosDocumentProviderInitialized()
    if (registrationStarted) {
        return
    }
    registrationStarted = true

    CoroutineScope(Dispatchers.Default).launch {
        registerIosDigitalCredentials()

        val documentStore: DocumentStore = IosDocumentProviderComponent.get()
        val settingsModel: AppSettingsModel = IosDocumentProviderComponent.get()

        documentStore.eventFlow
            .onEach {
                Logger.i(TAG, "DocumentStore updated, refreshing iOS W3C DC registrations")
                registerIosDigitalCredentials()
            }.launchIn(this)

        settingsModel.dcApiProtocols
            .drop(1)
            .onEach {
                Logger.i(TAG, "DC API protocols changed, refreshing iOS W3C DC registrations")
                registerIosDigitalCredentials()
            }.launchIn(this)
    }
}

@Suppress("FunctionName")
suspend fun UpdateIosDocumentProviderRegistrations() {
    EnsureIosDocumentProviderInitialized()
    registerIosDigitalCredentials()
}

@Suppress("FunctionName")
suspend fun ProcessIosDocumentRequest(
    requestData: String,
    origin: String?,
): String {
    EnsureIosDocumentProviderInitialized()
    val source: PresentmentSource = IosDocumentProviderComponent.get()
    return digitalCredentialsPresentment(
        protocol = "org-iso-mdoc",
        data = requestData,
        appId = null,
        origin = origin ?: "",
        preselectedDocuments = emptyList(),
        source = source,
    )
}

private suspend fun registerIosDigitalCredentials() {
    val digitalCredentials = DigitalCredentials.getDefault()
    if (!digitalCredentials.registerAvailable) {
        return
    }

    val documentStore: DocumentStore = IosDocumentProviderComponent.get()
    val settingsModel: AppSettingsModel = IosDocumentProviderComponent.get()
    val entitledRepository = createEntitledIosDocumentTypeRepository()
    val selectedProtocols =
        settingsModel.dcApiProtocols.value
            .intersect(setOf("org-iso-mdoc"))
            .ifEmpty { setOf("org-iso-mdoc") }

    try {
        val authState = digitalCredentials.authorizationState.value
        Logger.i(
            TAG,
            "Registering iOS DC credentials with protocols=$selectedProtocols authState=$authState",
        )
        digitalCredentials.register(
            documentStore = documentStore,
            documentTypeRepository = entitledRepository,
            selectedProtocols = selectedProtocols,
        )
    } catch (t: Throwable) {
        Logger.w(TAG, "Error registering with iOS W3C DC API", t)
    }
}

private fun createEntitledIosDocumentTypeRepository(): DocumentTypeRepository =
    DocumentTypeRepository().apply {
        // Keep in sync with iosApp entitlements mobile-document-types.
        addDocumentType(DrivingLicense.getDocumentType())
    }

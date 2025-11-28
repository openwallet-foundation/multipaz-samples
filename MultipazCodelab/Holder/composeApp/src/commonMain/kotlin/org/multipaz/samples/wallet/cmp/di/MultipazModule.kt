package org.multipaz.samples.wallet.cmp.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import org.koin.dsl.module
import org.multipaz.crypto.X509Cert
import org.multipaz.digitalcredentials.Default
import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.document.DocumentMetadata
import org.multipaz.document.DocumentStore
import org.multipaz.document.buildDocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.Loyalty
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.presentment.model.SimplePresentmentSource
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.prompt.PromptModel
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport
import org.multipaz.samples.wallet.cmp.util.TestAppUtils
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.storage.Storage
import org.multipaz.trustmanagement.TrustManager
import org.multipaz.trustmanagement.TrustManagerLocal
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.trustmanagement.TrustPointAlreadyExistsException
import org.multipaz.util.Logger
import org.multipaz.util.Platform
import utopiasample.composeapp.generated.resources.Res

val multipazModule = module {
    //TODO: define Storage in Koin module
    //TODO: define SecureArea in Koin module
    //TODO: define SecureAreaRepository in Koin module
    //TODO: define DocumentStore in Koin module
    single<PromptModel> {
        Platform.promptModel
    }
    single<HttpClient> {
        HttpClient {
            followRedirects = false
        }
    }

    //TODO: define TrustManager in Koin module

    //TODO: define PresentmentSource in Koin module

    single<ProvisioningModel> {
        ProvisioningModel(
            documentStore = get(),
            secureArea = get(),
            httpClient = get(),
            promptModel = get(),
            documentMetadataInitializer = { metadata, credentialDisplay, issuerDisplay ->
                (metadata as DocumentMetadata).setMetadata(
                    displayName = credentialDisplay.text,
                    typeDisplayName = credentialDisplay.text,
                    cardArt = credentialDisplay.logo
                        ?: ByteString(Res.readBytes("drawable/profile.png")),
                    issuerLogo = issuerDisplay.logo,
                    other = null
                )
            }
        )
    }


    single<ProvisioningSupport> {
        ProvisioningSupport()
    }

    single<PresentmentModel> {
        PresentmentModel().apply {
            setPromptModel(get())
        }
    }
}
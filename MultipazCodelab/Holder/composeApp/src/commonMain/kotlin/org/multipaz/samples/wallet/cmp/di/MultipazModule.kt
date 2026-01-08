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
    //TODO: define DocumentTypeRepository in Koin module
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

    single<TrustManager> {
        val trustManager = TrustManagerLocal(storage = get(), identifier = "reader")

        runBlocking {
            suspend fun addCertificateIfNotExists(
                certPath: String,
                displayName: String,
                privacyPolicyUrl: String
            ) {
                try {
                    val certPem = Res.readBytes(certPath)
                        .decodeToString()
                        .trimIndent()
                        .trim()

                    trustManager.addX509Cert(
                        certificate = X509Cert.fromPem(certPem),
                        metadata = TrustMetadata(
                            displayName = displayName,
                            displayIcon = null,
                            privacyPolicyUrl = privacyPolicyUrl
                        )
                    )
                    Logger.i("TrustManager", "Successfully added certificate: $displayName")
                } catch (e: TrustPointAlreadyExistsException) {
                    Logger.e(
                        "TrustManager",
                        "Certificate already exists: $displayName",
                        e)
                } catch (e: Exception) {
                    Logger.e(
                        "TrustManager",
                        "Failed to add certificate: $displayName - ${e.message}",
                        e
                    )
                }
            }

            // Add all required certificates
            addCertificateIfNotExists(
                certPath = "files/test_app_reader_root_certificate.pem",
                displayName = "OWF Multipaz Test App Reader",
                privacyPolicyUrl = "https://apps.multipaz.org"
            )

            addCertificateIfNotExists(
                certPath = "files/reader_root_certificate.pem",
                displayName = "Multipaz Identity Reader (Trusted Devices)",
                privacyPolicyUrl = "https://apps.multipaz.org"
            )

            addCertificateIfNotExists(
                certPath = "files/reader_root_certificate_for_untrust_device.pem",
                displayName = "Multipaz Identity Reader (UnTrusted Devices)",
                privacyPolicyUrl = "https://apps.multipaz.org"
            )

            trustManager
        }
    }

    single<PresentmentSource> {
        runBlocking {
            if (DigitalCredentials.Default.available) {
                DigitalCredentials.Default.startExportingCredentials(
                    documentStore = get(),
                    documentTypeRepository = get()
                )
            }

            SimplePresentmentSource(
                documentStore = get(),
                documentTypeRepository = get(),
                readerTrustManager = get(),
                preferSignatureToKeyAgreement = true,
                // Match domains used when storing credentials via OpenID4VCI
                domainMdocSignature = TestAppUtils.CREDENTIAL_DOMAIN_MDOC_USER_AUTH,
                domainMdocKeyAgreement = TestAppUtils.CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH,
                domainKeylessSdJwt = TestAppUtils.CREDENTIAL_DOMAIN_SDJWT_KEYLESS,
                domainKeyBoundSdJwt = TestAppUtils.CREDENTIAL_DOMAIN_SDJWT_USER_AUTH
            )
        }
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

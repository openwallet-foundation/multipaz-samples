@file:OptIn(kotlin.time.ExperimentalTime::class)

package org.multipaz.samples.wallet.cmp.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.dsl.module
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.X509Cert
import org.multipaz.document.DocumentStore
import org.multipaz.document.buildDocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.Loyalty
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.presentment.model.SimplePresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.prompt.promptModelRequestConsent
import org.multipaz.prompt.promptModelSilentConsent
import org.multipaz.provisioning.DocumentProvisioningHandler
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.provisioning.openid4vci.OpenID4VCIBackend
import org.multipaz.provisioning.openid4vci.OpenID4VCIClientPreferences
import org.multipaz.rpc.handler.RpcAuthClientSession
import org.multipaz.samples.wallet.cmp.util.AppSettingsModel
import org.multipaz.samples.wallet.cmp.util.DigitalCredentialsRegistrationManager
import org.multipaz.samples.wallet.cmp.util.OpenID4VCILocalBackend
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport.Companion.APP_LINK_BASE_URL
import org.multipaz.samples.wallet.cmp.util.TestAppUtils
import org.multipaz.samples.wallet.cmp.util.createWalletStorage
import org.multipaz.samples.wallet.cmp.util.shouldRegisterDigitalCredentialsInCommonModule
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

val multipazModule =
    module {
        single<Storage> { createWalletStorage() }
        single<SecureArea> { runBlocking { Platform.getSecureArea() } }
        single<SecureAreaRepository> {
            val secureArea: SecureArea = get()
            SecureAreaRepository
                .Builder()
                .add(secureArea).build()
        }
        single<DocumentTypeRepository> {
            DocumentTypeRepository().apply {
                addDocumentType(DrivingLicense.getDocumentType())
                addDocumentType(Loyalty.getDocumentType())
            }
        }
        single<DocumentStore> {
            buildDocumentStore(
                storage = get(),
                secureAreaRepository = get(),
            ) {}
        }
        single<PromptModel> {
            Platform.promptModel
        }
        single<HttpClient> {
            HttpClient {
                followRedirects = false
            }
        }
        single<AppSettingsModel> {
            runBlocking {
                AppSettingsModel.create(
                    storage = get(),
                    readOnly = false,
                )
            }
        }
        single<ProvisioningModel> {
            val secureArea: SecureArea = get()
            ProvisioningModel(
                documentProvisioningHandler =
                    DocumentProvisioningHandler(
                        secureArea = secureArea,
                        documentStore = get(),
                    ),
                httpClient = get(),
                promptModel = get(),
                authorizationSecureArea = secureArea,
            )
        }

        single<TrustManager> {
            val trustManager = TrustManagerLocal(storage = get(), identifier = "reader")

            runBlocking {
                suspend fun addCertificateIfNotExists(
                    certPath: String,
                    displayName: String,
                    privacyPolicyUrl: String,
                ) {
                    try {
                        val certPem =
                            Res.readBytes(certPath)
                                .decodeToString()
                                .trimIndent()
                                .trim()

                        trustManager.addX509Cert(
                            certificate = X509Cert.fromPem(certPem),
                            metadata =
                                TrustMetadata(
                                    displayName = displayName,
                                    displayIcon = null,
                                    privacyPolicyUrl = privacyPolicyUrl,
                                ),
                        )
                        Logger.i("TrustManager", "Successfully added certificate: $displayName")
                    } catch (e: TrustPointAlreadyExistsException) {
                        Logger.e(
                            "TrustManager",
                            "Certificate already exists: $displayName",
                            e,
                        )
                    } catch (e: Exception) {
                        Logger.e(
                            "TrustManager",
                            "Failed to add certificate: $displayName - ${e.message}",
                            e,
                        )
                    }
                }

                // Add all required certificates
                addCertificateIfNotExists(
                    certPath = "files/test_app_reader_root_certificate.pem",
                    displayName = "OWF Multipaz Test App Reader",
                    privacyPolicyUrl = "https://apps.multipaz.org",
                )

                addCertificateIfNotExists(
                    certPath = "files/reader_root_certificate.pem",
                    displayName = "Multipaz Identity Reader (Trusted Devices)",
                    privacyPolicyUrl = "https://apps.multipaz.org",
                )

                addCertificateIfNotExists(
                    certPath = "files/reader_root_certificate_for_untrust_device.pem",
                    displayName = "Multipaz Identity Reader (UnTrusted Devices)",
                    privacyPolicyUrl = "https://apps.multipaz.org",
                )

                addCertificateIfNotExists(
                    certPath = "files/reader_root_cert_multipaz_web_verifier.pem",
                    displayName = "Multipaz Web Verifier",
                    privacyPolicyUrl = "https://verifier.multipaz.org",
                )

                trustManager
            }
        }

        single<DigitalCredentialsRegistrationManager> {
            DigitalCredentialsRegistrationManager(
                documentStore = get(),
                documentTypeRepository = get(),
                settingsModel = get(),
            )
        }

        single<PresentmentSource> {
            val settingsModel: AppSettingsModel = get()
            val requireAuthentication = settingsModel.presentmentRequireAuthentication.value
            val documentStore: DocumentStore = get()
            val documentTypeRepository: DocumentTypeRepository = get()

            // Keep an initial eager refresh here so existing startup behavior is preserved.
            if (shouldRegisterDigitalCredentialsInCommonModule()) {
                runBlocking { get<DigitalCredentialsRegistrationManager>().refresh("PresentmentSource init") }
            }

            SimplePresentmentSource(
                documentStore = documentStore,
                documentTypeRepository = documentTypeRepository,
                showConsentPromptFn =
                    if (settingsModel.presentmentShowConsentPrompt.value) {
                        ::promptModelRequestConsent
                    } else {
                        ::promptModelSilentConsent
                    },
                resolveTrustFn = { requester ->
                    requester.certChain?.let { certChain ->
                        val trustResult = get<TrustManager>().verify(certChain.certificates)
                        if (trustResult.isTrusted) {
                            trustResult.trustPoints.firstOrNull()?.metadata
                        } else {
                            null
                        }
                    }
                },
                preferSignatureToKeyAgreement = settingsModel.presentmentPreferSignatureToKeyAgreement.value,
                domainMdocSignature =
                    if (requireAuthentication) {
                        TestAppUtils.CREDENTIAL_DOMAIN_MDOC_USER_AUTH
                    } else {
                        TestAppUtils.CREDENTIAL_DOMAIN_MDOC_NO_USER_AUTH
                    },
                domainMdocKeyAgreement =
                    if (requireAuthentication) {
                        TestAppUtils.CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH
                    } else {
                        TestAppUtils.CREDENTIAL_DOMAIN_MDOC_MAC_NO_USER_AUTH
                    },
                domainKeylessSdJwt = TestAppUtils.CREDENTIAL_DOMAIN_SDJWT_KEYLESS,
                domainKeyBoundSdJwt =
                    if (requireAuthentication) {
                        TestAppUtils.CREDENTIAL_DOMAIN_SDJWT_USER_AUTH
                    } else {
                        TestAppUtils.CREDENTIAL_DOMAIN_SDJWT_NO_USER_AUTH
                    },
            )
        }

        single<ProvisioningSupport> {
            ProvisioningSupport(
                get(),
                get(),
                get(),
                get(),
            )
        }

        single<OpenID4VCIBackend> {
            OpenID4VCILocalBackend()
        }

        single<OpenID4VCIClientPreferences> {
            OpenID4VCIClientPreferences(
                clientId =
                    runBlocking {
                        withContext(RpcAuthClientSession()) {
                            get<OpenID4VCIBackend>().getClientId()
                        }
                    },
                redirectUrl = APP_LINK_BASE_URL,
                locales = listOf("en-US"),
                signingAlgorithms = listOf(Algorithm.ESP256, Algorithm.ESP384, Algorithm.ESP512),
            )
        }

        single<PresentmentModel> {
            PresentmentModel()
        }
    }

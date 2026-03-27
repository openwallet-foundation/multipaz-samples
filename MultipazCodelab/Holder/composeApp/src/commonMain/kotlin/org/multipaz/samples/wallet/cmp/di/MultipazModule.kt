@file:OptIn(kotlin.time.ExperimentalTime::class)

package org.multipaz.samples.wallet.cmp.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.dsl.module
import org.multipaz.crypto.Algorithm
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.prompt.PromptModel
import org.multipaz.provisioning.DocumentProvisioningHandler
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.provisioning.openid4vci.OpenID4VCIBackend
import org.multipaz.provisioning.openid4vci.OpenID4VCIClientPreferences
import org.multipaz.rpc.handler.RpcAuthClientSession
import org.multipaz.samples.wallet.cmp.util.AppSettingsModel
import org.multipaz.samples.wallet.cmp.util.OpenID4VCILocalBackend
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport.Companion.APP_LINK_BASE_URL
import org.multipaz.securearea.SecureArea
import org.multipaz.util.Platform

val multipazModule =
    module {
        // TODO: define Storage in Koin module
        // TODO: define SecureArea in Koin module
        // TODO: define SecureAreaRepository in Koin module
        // TODO: define DocumentStore in Koin module
        // TODO: define DocumentTypeRepository in Koin module
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
        // TODO: define TrustManager in Koin module

        // TODO: define DigitalCredentialsRegistrationManager in Koin module

        // TODO: define PresentmentSource in Koin module

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

package org.multipaz.getstarted.core

import io.ktor.client.engine.HttpClientEngineFactory
import org.multipaz.digitalcredentials.DigitalCredentials

expect suspend fun getAppToAppOrigin(): String

expect fun isAndroid(): Boolean

expect suspend fun getDigitalCredentials(): DigitalCredentials

expect val httpClientEngineFactory: HttpClientEngineFactory<*>

package org.multipaz.getstarted

import io.ktor.client.engine.HttpClientEngineFactory

expect suspend fun getAppToAppOrigin(): String

expect fun isAndroid(): Boolean

expect val httpClientEngineFactory: HttpClientEngineFactory<*>
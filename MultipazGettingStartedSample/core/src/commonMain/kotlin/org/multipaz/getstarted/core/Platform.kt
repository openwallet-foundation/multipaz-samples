package org.multipaz.getstarted.core

import io.ktor.client.engine.HttpClientEngineFactory

expect suspend fun getAppToAppOrigin(): String

expect fun isAndroid(): Boolean

expect val httpClientEngineFactory: HttpClientEngineFactory<*>

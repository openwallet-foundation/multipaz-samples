package org.multipaz.getstarted.core

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.digitalcredentials.DigitalCredentialsIos
import platform.Foundation.NSBundle

actual fun isAndroid(): Boolean = false

actual suspend fun getAppToAppOrigin(): String {
    return NSBundle.mainBundle.bundleIdentifier ?: "unknown.bundle.id"
}

actual suspend fun getDigitalCredentials(): DigitalCredentials {
    return DigitalCredentialsIos.create()
}

actual val httpClientEngineFactory: HttpClientEngineFactory<*> by lazy {
    Darwin
}

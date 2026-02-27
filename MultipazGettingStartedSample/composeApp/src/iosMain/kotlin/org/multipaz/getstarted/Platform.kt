package org.multipaz.getstarted

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSBundle

actual fun isAndroid(): Boolean = false


actual suspend fun getAppToAppOrigin(): String {
    // On iOS, use the bundle identifier as the app origin
    // This uniquely identifies the app and is the iOS equivalent
    // of using the signing certificate on Android
    return NSBundle.mainBundle.bundleIdentifier ?: "unknown.bundle.id"
}

actual val httpClientEngineFactory: HttpClientEngineFactory<*> by lazy {
    Darwin
}
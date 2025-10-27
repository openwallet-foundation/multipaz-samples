package org.multipaz.photoididentityreader

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import platform.UIKit.UIDevice
import platform.posix.exit

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override fun exitApp() {
        exit(0)
    }
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun platformHttpClientEngineFactory(): HttpClientEngineFactory<*> = Darwin

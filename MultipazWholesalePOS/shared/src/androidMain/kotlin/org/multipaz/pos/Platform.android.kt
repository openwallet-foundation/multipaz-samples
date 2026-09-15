package org.multipaz.pos

import io.ktor.client.engine.android.Android
import io.ktor.client.engine.HttpClientEngineFactory

actual object Platform {
    actual val httpClientEngineFactory: HttpClientEngineFactory<*> by lazy {
        Android
    }
}
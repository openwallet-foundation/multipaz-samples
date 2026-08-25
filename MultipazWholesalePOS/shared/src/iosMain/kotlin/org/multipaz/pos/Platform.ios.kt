package org.multipaz.pos

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual object Platform {
    actual val httpClientEngineFactory: HttpClientEngineFactory<*> by lazy {
        Darwin
    }
}
package org.multipaz.pos

import io.ktor.client.engine.HttpClientEngineFactory

expect object Platform {
    val httpClientEngineFactory: HttpClientEngineFactory<*>
}
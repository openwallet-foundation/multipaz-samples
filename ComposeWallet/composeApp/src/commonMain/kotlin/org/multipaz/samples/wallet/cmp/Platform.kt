package org.multipaz.samples.wallet.cmp

import io.ktor.client.engine.HttpClientEngineFactory

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun platformHttpClientEngineFactory(): HttpClientEngineFactory<*>
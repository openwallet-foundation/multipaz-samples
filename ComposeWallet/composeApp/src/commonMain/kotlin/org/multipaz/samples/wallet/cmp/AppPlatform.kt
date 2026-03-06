package org.multipaz.samples.wallet.cmp

import io.ktor.client.engine.HttpClientEngineFactory
import org.multipaz.storage.Storage

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppPlatform {
    val storage: Storage

    val redirectPath: String

    val httpClientEngineFactory: HttpClientEngineFactory<*>
}
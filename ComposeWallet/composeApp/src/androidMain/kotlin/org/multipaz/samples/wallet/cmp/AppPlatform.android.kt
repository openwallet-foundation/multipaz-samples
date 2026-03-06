package org.multipaz.samples.wallet.cmp

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android
import org.multipaz.context.applicationContext
import org.multipaz.storage.Storage
import org.multipaz.util.Platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object AppPlatform {

    actual val storage: Storage = Platform.nonBackedUpStorage

    actual val redirectPath: String = "/redirect/${applicationContext.packageName}/"

    actual val httpClientEngineFactory: HttpClientEngineFactory<*> by lazy {
        Android
    }
}

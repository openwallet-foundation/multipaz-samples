package org.multipaz.samples.wallet.cmp

import org.multipaz.compose.presentment.UriSchemePresentmentActivity

class UriSchemePresentmentActivity: UriSchemePresentmentActivity() {
    override suspend fun getSettings(): Settings {
        val app = App.getInstance()
        app.init()
        return Settings(
            source = app.presentmentSource,
            httpClientEngineFactory = AppPlatform.httpClientEngineFactory,
        )
    }
}

package org.multipaz.samples.wallet.cmp

import mpzcmpwallet.composeapp.generated.resources.Res
import org.multipaz.compose.digitalcredentials.CredentialManagerPresentmentActivity

class CredmanActivity: CredentialManagerPresentmentActivity() {
    override suspend fun getSettings(): Settings {
        val app = App.getInstance()
        app.init()
        return Settings(
            source = app.presentmentSource,
            privilegedAllowList = Res.readBytes("files/privilegedUserAgents.json").decodeToString()
        )
    }
}
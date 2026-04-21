package org.multipaz.getstarted

import multipazgettingstartedsample.composeapp.generated.resources.Res
import org.multipaz.compose.digitalcredentials.CredentialManagerPresentmentActivity
import org.multipaz.getstarted.core.AppContainer

class CredmanActivity : CredentialManagerPresentmentActivity() {
    override suspend fun getSettings(): Settings {
        val container = AppContainer.getInstance()
        container.init()
        return Settings(
            source = container.presentmentSource,
            privilegedAllowList = Res.readBytes("files/privilegedUserAgents.json").decodeToString()
        )
    }
}

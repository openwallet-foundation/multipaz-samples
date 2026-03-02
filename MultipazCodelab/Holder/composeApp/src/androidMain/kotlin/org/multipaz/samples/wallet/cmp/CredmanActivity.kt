package org.multipaz.samples.wallet.cmp

import org.koin.android.ext.android.inject
import org.multipaz.compose.digitalcredentials.CredentialManagerPresentmentActivity
import org.multipaz.presentment.model.PresentmentSource
import utopiasample.composeapp.generated.resources.Res

class CredmanActivity : CredentialManagerPresentmentActivity() {
    private val presentmentSource: PresentmentSource by inject()

    override suspend fun getSettings(): Settings {
        return Settings(
            source = presentmentSource,
            privilegedAllowList = Res.readBytes("files/privilegedUserAgents.json").decodeToString(),
        )
    }
}

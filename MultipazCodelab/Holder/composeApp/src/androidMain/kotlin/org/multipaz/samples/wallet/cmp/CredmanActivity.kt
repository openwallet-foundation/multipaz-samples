package org.multipaz.samples.wallet.cmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import org.multipaz.compose.digitalcredentials.CredentialManagerPresentmentActivity
import utopiasample.composeapp.generated.resources.Res

class CredmanActivity: CredentialManagerPresentmentActivity() {
    override suspend fun getSettings(): Settings {
        val app = App.getInstance()
        app.init()
        return Settings(
            appName = app.appName,
            appIcon = app.appIcon,
            promptModel = App.promptModel,
            applicationTheme = @Composable { content -> MaterialTheme { content() } },
            documentTypeRepository = app.documentTypeRepository,
            presentmentSource = app.presentmentSource,
            imageLoader = ImageLoader.Builder(applicationContext).components { /* network loader omitted */ }.build(),
            privilegedAllowList = Res.readBytes("files/privilegedUserAgents.json").decodeToString()
        )
    }

}
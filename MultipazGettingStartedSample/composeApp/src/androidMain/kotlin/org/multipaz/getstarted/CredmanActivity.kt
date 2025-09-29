package org.multipaz.getstarted

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import multipazgettingstartedsample.composeapp.generated.resources.Res
import org.multipaz.compose.digitalcredentials.CredentialManagerPresentmentActivity

class CredmanActivity : CredentialManagerPresentmentActivity() {
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
            privilegedAllowList = Res.readBytes("files/privilegedUserAgents.json").decodeToString(),
            imageLoader = ImageLoader.Builder(applicationContext)
                .components { /* network loader omitted */ }.build(),
        )
    }
}
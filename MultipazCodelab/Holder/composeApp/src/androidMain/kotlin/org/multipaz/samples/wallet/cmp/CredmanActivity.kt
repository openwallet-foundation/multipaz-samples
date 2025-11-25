package org.multipaz.samples.wallet.cmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import org.koin.android.ext.android.inject
import org.multipaz.compose.digitalcredentials.CredentialManagerPresentmentActivity
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.samples.wallet.cmp.util.Constants.APP_NAME
import org.multipaz.samples.wallet.cmp.util.Constants.appIcon
import utopiasample.composeapp.generated.resources.Res

class CredmanActivity: CredentialManagerPresentmentActivity() {

    private val promptModel : PromptModel by inject()
    private val documentTypeRepository : DocumentTypeRepository by inject()
    private val presentmentSource: PresentmentSource by inject()


    override suspend fun getSettings(): Settings {
        return Settings(
            appName = APP_NAME,
            appIcon = appIcon,
            promptModel = promptModel,
            applicationTheme = @Composable { content -> MaterialTheme { content() } },
            documentTypeRepository = documentTypeRepository,
            presentmentSource = presentmentSource,
            imageLoader = ImageLoader.Builder(applicationContext).components { /* network loader omitted */ }.build(),
            privilegedAllowList = Res.readBytes("files/privilegedUserAgents.json").decodeToString()
        )
    }
}
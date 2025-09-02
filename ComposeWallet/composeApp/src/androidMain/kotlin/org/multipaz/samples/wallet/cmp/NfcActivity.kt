package org.multipaz.samples.wallet.cmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import org.multipaz.compose.mdoc.MdocNfcPresentmentActivity

class NfcActivity: MdocNfcPresentmentActivity() {
    override suspend fun getSettings(): Settings {
        val app = App.getInstance()
        app.init()
        return Settings(
            appName = app.appName,
            appIcon = app.appIcon,
            promptModel = App.promptModel,
            applicationTheme = @Composable { content -> MaterialTheme { content() } },
            documentTypeRepository = app.documentTypeRepository,
            imageLoader = ImageLoader.Builder(applicationContext).components { /* network loader omitted */ }.build(),
            presentmentSource = app.presentmentSource
        )
    }
}
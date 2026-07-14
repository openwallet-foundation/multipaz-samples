package org.multipaz.samples.wallet.cmp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.multipaz.context.initializeApplication
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport
import org.multipaz.samples.wallet.cmp.util.handleUrl

class MainActivity : FragmentActivity() {
    private val provisioningModel: ProvisioningModel by inject()
    private val provisioningSupport: ProvisioningSupport by inject()
    private val credentialOffers = Channel<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        initializeApplication(this.applicationContext)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            UtopiaSampleApp(
                credentialOffers = credentialOffers,
                provisioningModel = provisioningModel,
                provisioningSupport = provisioningSupport,
            )
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (url != null) {
                lifecycle.coroutineScope.launch {
                    handleUrl(url)
                }
            }
        }
    }

    /**
     * Handle a link (either a app link, universal link, or custom URL schema link).
     */
    fun handleUrl(url: String) {
        handleUrl(
            url = url,
            credentialOffers = credentialOffers,
            provisioningModel = provisioningModel,
            provisioningSupport = provisioningSupport,
        )
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

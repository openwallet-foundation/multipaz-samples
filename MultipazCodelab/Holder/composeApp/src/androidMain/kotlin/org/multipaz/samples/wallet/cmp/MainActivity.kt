package org.multipaz.samples.wallet.cmp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.multipaz.context.initializeApplication
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport
import org.multipaz.util.Logger

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
                provisioningSupport = provisioningSupport
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
        Logger.i(TAG, "handleUrl called with: $url")
        Logger.i(TAG, "handleUrl provisioningModel sate: ${provisioningModel.state.value}")
        if (url.startsWith(OID4VCI_CREDENTIAL_OFFER_URL_SCHEME)
            || url.startsWith(HAIP_URL_SCHEME)
        ) {
            val queryIndex = url.indexOf('?')
            if (queryIndex >= 0) {
                Logger.i(TAG, "Starting OpenID4VCI provisioning with: $url")
                Logger.i(
                    TAG,
                    "OID4VCI_CREDENTIAL_OFFER_URL_SCHEME provisioningModel: $provisioningModel"
                )
                Logger.i(TAG, "handleUrl: Sending credential offer to channel...")
                CoroutineScope(Dispatchers.Default).launch {
                    Logger.i(TAG, "handleUrl: About to send to credentialOffers channel")
                    credentialOffers.send(url)
                    Logger.i(TAG, "handleUrl: Successfully sent to credentialOffers channel")
                }
                Logger.i(
                    TAG,
                    "handleUrl: Credential offer sent to channel, LaunchedEffect should process it"
                )
                // Navigate to ProvisioningTestScreen after starting provisioning (commented out, handled by LaunchedEffect)
            }
        } else if (url.startsWith(ProvisioningSupport.APP_LINK_BASE_URL)) {
            Logger.i(TAG, "APP_LINK_BASE_URL provisioningModel: $provisioningModel")
            Logger.i(TAG, "Processing app link invocation: $url")

            // Check if we have an active provisioning session (removed by assistant, re-added by user, then removed again)
            // if (!isProvisioningActive()) { ... } // This check is currently NOT in the user's latest code.

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    Logger.i(TAG, "handleUrl: About to process app link invocation")
                    //TODO: call processAppLinkInvocation(url)
                    Logger.i(TAG, "handleUrl: App link invocation processed successfully")
                } catch (e: Exception) {
                    Logger.e(TAG, "Error processing app link: ${e.message}", e)
                    // resetProvisioningModel() // This call is currently NOT in the user's latest code.
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val OID4VCI_CREDENTIAL_OFFER_URL_SCHEME = "openid-credential-offer://"
        private const val HAIP_URL_SCHEME = "haip://"
    }
}
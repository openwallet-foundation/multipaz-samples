package org.multipaz.samples.wallet.cmp.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.util.Logger

private const val TAG = "UrlHandler"
private const val OID4VCI_CREDENTIAL_OFFER_URL_SCHEME = "openid-credential-offer://"
private const val HAIP_URL_SCHEME = "haip://"

/**
 * Handle a link (either an app link, universal link, or custom URL scheme link).
 * This is a common handler that can be used from both Android and iOS.
 *
 * @param url The URL to handle
 * @param credentialOffers Channel to send credential offers to
 * @param provisioningModel The provisioning model instance
 * @param provisioningSupport The provisioning support instance
 */
fun handleUrl(
    url: String,
    credentialOffers: Channel<String>,
    provisioningModel: ProvisioningModel,
    provisioningSupport: ProvisioningSupport
) {
    Logger.i(TAG, "handleUrl called with: $url")
    Logger.i(TAG, "handleUrl provisioningModel state: ${provisioningModel.state.value}")
    
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
        }
    } else if (url.startsWith(ProvisioningSupport.APP_LINK_BASE_URL)) {
        Logger.i(TAG, "APP_LINK_BASE_URL provisioningModel: $provisioningModel")
        Logger.i(TAG, "Processing app link invocation: $url")

        CoroutineScope(Dispatchers.Default).launch {
            try {
                Logger.i(TAG, "handleUrl: About to process app link invocation")
                provisioningSupport.processAppLinkInvocation(url)
                Logger.i(TAG, "handleUrl: App link invocation processed successfully")
            } catch (e: Exception) {
                Logger.e(TAG, "Error processing app link: ${e.message}", e)
            }
        }
    }
}



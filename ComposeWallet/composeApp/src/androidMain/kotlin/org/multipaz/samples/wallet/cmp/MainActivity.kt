package org.multipaz.samples.wallet.cmp

import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import org.multipaz.applinks.AppLinksCheck
import org.multipaz.context.initializeApplication
import org.multipaz.util.Logger

class MainActivity : FragmentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onResume() {
        super.onResume()
        NfcAdapter.getDefaultAdapter(this)?.let { adapter ->
            val cardEmulation = CardEmulation.getInstance(adapter)
            val componentName = ComponentName(this, NdefService::class.java)
            if (!cardEmulation.setPreferredService(this, componentName)) {
                Logger.w(TAG, "CardEmulation.setPreferredService() returned false")
            }
            if (!cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)) {
                Logger.w(TAG, "CardEmulation.categoryAllowsForegroundPreference(CATEGORY_OTHER) returned false")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.let {
            val cardEmulation = CardEmulation.getInstance(it)
            if (!cardEmulation.unsetPreferredService(this)) {
                Logger.w(TAG, "CardEmulation.unsetPreferredService() return false")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initializeApplication(this.applicationContext)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        lifecycle.coroutineScope.launch {
            val app = App.getInstance()
            app.init()
            setContent {
                app.Content()
            }
            AppLinksCheck.checkAppLinksServerSetup(
                context = this@MainActivity,
                appLinkServer = ProvisioningSupport.APP_LINK_SERVER,
                httpClient = HttpClient(platformHttpClientEngineFactory())
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (url != null) {
                lifecycle.coroutineScope.launch {
                    val app = App.getInstance()
                    app.init()
                    app.handleUrl(url)
                }
            }
        }
    }
}

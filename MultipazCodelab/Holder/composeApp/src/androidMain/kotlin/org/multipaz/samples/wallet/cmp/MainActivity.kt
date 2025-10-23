package org.multipaz.samples.wallet.cmp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.launch
import org.multipaz.context.initializeApplication

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        initializeApplication(this.applicationContext)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycle.coroutineScope.launch {
            val app = App.getInstance()
            app.init()
            setContent {
                app.Content()
            }
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
                    val app = App.getInstance()
                    app.init()
                    app.handleUrl(url)
                }
            }
        }
    }
}

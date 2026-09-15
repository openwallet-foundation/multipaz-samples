package org.multipaz.pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import org.multipaz.context.initializeApplication

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        initializeApplication(applicationContext)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}
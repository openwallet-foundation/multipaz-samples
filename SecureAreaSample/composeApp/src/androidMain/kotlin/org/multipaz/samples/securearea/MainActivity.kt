package org.multipaz.samples.securearea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.multipaz.context.initializeApplication
import org.multipaz.prompt.AndroidPromptModel

private val promptModel = AndroidPromptModel()

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeApplication(this.applicationContext)

        setContent {
            App(promptModel)
        }
    }
}

package org.multipaz.samples.wallet.cmp

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.runBlocking
import org.multipaz.prompt.IosPromptModel

private val app = App.getInstance()

fun MainViewController() = ComposeUIViewController {
  app.Content()
}

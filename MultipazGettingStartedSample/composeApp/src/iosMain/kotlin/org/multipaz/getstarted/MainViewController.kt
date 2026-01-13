package org.multipaz.getstarted

import androidx.compose.ui.window.ComposeUIViewController

private val app = App.getInstance()

fun MainViewController() = ComposeUIViewController {
    app.Content()
}

fun HandleUrl(url: String) {
    app.handleUrl(url)
}
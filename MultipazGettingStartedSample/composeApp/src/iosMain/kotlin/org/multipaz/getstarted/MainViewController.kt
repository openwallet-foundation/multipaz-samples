package org.multipaz.getstarted

import androidx.compose.ui.window.ComposeUIViewController

private val app = App.getInstance()

fun MainViewController() = ComposeUIViewController {
    app.Content()
}
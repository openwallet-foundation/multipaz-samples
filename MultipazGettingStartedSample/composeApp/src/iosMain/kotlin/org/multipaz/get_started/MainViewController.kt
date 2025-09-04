package org.multipaz.get_started

import androidx.compose.ui.window.ComposeUIViewController

private val app = App.getInstance()

fun MainViewController() = ComposeUIViewController {
    app.Content()
}
package org.multipaz.photoididentityreader

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.runBlocking

private val app = App(urlLaunchData = null).also {
    runBlocking { it.initialize() }
}

fun MainViewController() = ComposeUIViewController { app.Content() }
package org.multipaz.samples.securearea

import androidx.compose.ui.window.ComposeUIViewController
import org.multipaz.prompt.IosPromptModel

private val promptModel = IosPromptModel()

fun MainViewController() = ComposeUIViewController { App(promptModel) }
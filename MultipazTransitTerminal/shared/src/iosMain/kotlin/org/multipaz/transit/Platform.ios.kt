package org.multipaz.transit

import androidx.compose.ui.text.font.FontFamily
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual object Platform {
    actual val httpClientEngineFactory: HttpClientEngineFactory<*> by lazy {
        Darwin
    }
}

/** iOS ships no humanist sans, so the kiosk uses the system face (San Francisco). */
actual fun kioskFontFamily(): FontFamily = FontFamily.SansSerif

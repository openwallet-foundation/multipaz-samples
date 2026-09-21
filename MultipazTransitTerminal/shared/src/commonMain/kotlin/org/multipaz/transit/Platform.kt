package org.multipaz.transit

import androidx.compose.ui.text.font.FontFamily
import io.ktor.client.engine.HttpClientEngineFactory

expect object Platform {
    val httpClientEngineFactory: HttpClientEngineFactory<*>
}

/**
 * The kiosk face. The design is drawn in Source Sans 3, which no platform ships, so each
 * target supplies the closest font it already has rather than the app carrying binaries.
 */
expect fun kioskFontFamily(): FontFamily

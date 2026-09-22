package org.multipaz.transit

import android.os.Build
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

actual object Platform {
    actual val httpClientEngineFactory: HttpClientEngineFactory<*> by lazy {
        Android
    }
}

/**
 * AOSP has shipped Source Sans Pro in /system/fonts since Android 12, registered as the
 * `source-sans-pro` family with real 400/600/700 faces. It is the direct predecessor of the
 * design's Source Sans 3 and reads identically at UI sizes, so the kiosk gets its typeface
 * without the app bundling a single byte of font.
 *
 * These are optional-local fonts: on a device that lacks the family (Android 10/11, or a ROM
 * that stripped it) each one fails to resolve and Compose falls through to the platform
 * default, which is Roboto.
 */
actual fun kioskFontFamily(): FontFamily {
    val name = DeviceFontFamilyName("source-sans-pro")
    return FontFamily(
        Font(name, FontWeight.Normal),
        Font(name, FontWeight.Medium),
        Font(name, FontWeight.SemiBold),
        Font(name, FontWeight.Bold),
    )
}

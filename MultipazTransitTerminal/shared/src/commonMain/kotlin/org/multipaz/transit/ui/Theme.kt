package org.multipaz.transit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.multipaz.transit.kioskFontFamily

/**
 * The Utopia Transit kiosk palette: warm paper surfaces, a single desaturated teal for
 * everything interactive, and a muted green reserved for "the gate is open".
 */
@Immutable
data class TransitColors(
    /** Page behind the kiosk surface. */
    val background: Color = Color(0xFFE9E7E2),
    /** The kiosk surface itself. */
    val surface: Color = Color(0xFFFAF9F6),
    /** Raised rows and cards sitting on [surface]. */
    val card: Color = Color(0xFFFFFFFF),

    /** Primary text. Lighter text is this colour at reduced alpha. */
    val ink: Color = Color(0xFF1C1E1D),

    /** Brand teal: rings, the tap target, the logo mark. */
    val primary: Color = Color(0xFF277676),
    /** Deeper teal for numerals and text that has to hold contrast. */
    val primaryInk: Color = Color(0xFF006363),
    /** Teal that shows up on a pressed/hovered row border. */
    val primaryBorder: Color = Color(0xFF599291),

    /** Hairline around resting rows and chips. */
    val border: Color = Color(0xFFDCE7E7),
    /** Barely-there teal wash for a pressed row. */
    val tintLow: Color = Color(0xFFF4FCFC),
    /** Teal wash behind the destination summary. */
    val tint: Color = Color(0xFFEBF6F6),
    /** Fill inside the NFC target. */
    val tapFill: Color = Color(0xFFECF8F7),
    /** Hairline around the NFC target. */
    val tapBorder: Color = Color(0xFFCEE3E2),
    /** Unfilled portion of a progress track. */
    val track: Color = Color(0xFFDDEBEB),
    /** The resting (unread) document dot. */
    val dotIdle: Color = Color(0xFFCDDBDA),

    /** Document chip while its credential is being read. */
    val readingBorder: Color = Color(0xFFB4DAD9),
    val readingBackground: Color = Color(0xFFE7FAF9),

    /** Gate-open green. */
    val success: Color = Color(0xFF3A8357),
    val successInk: Color = Color(0xFF236E44),
    val successInkDeep: Color = Color(0xFF1F613C),
    val successBorder: Color = Color(0xFFC4E0CC),
    val successBackground: Color = Color(0xFFE5FAEB),

    /** Declined / gate-closed red, tuned to the same muted family as the greens. */
    val error: Color = Color(0xFFB54A46),
    val errorInk: Color = Color(0xFF932B2A),
    val errorBorder: Color = Color(0xFFF7CBC7),
    val errorBackground: Color = Color(0xFFFFEFED),
)

val LocalTransitColors = staticCompositionLocalOf { TransitColors() }

/** Convenience accessor: `TransitTheme.colors.primary`. */
object TransitTheme {
    val colors: TransitColors
        @Composable get() = LocalTransitColors.current
}

/**
 * The kiosk type scale. Sizes mirror the design one-for-one; the face comes from
 * [kioskFontFamily], which is Source Sans on any platform that already ships it.
 */
@Immutable
data class TransitTypography(
    /** Screen question, e.g. "Where are you going?". */
    val display: TextStyle,
    /** Headline over the tap target and the outcome screens. */
    val headline: TextStyle,
    /** Station name, and any row that reads as a title. */
    val title: TextStyle,
    /** The fare on a station row. */
    val fare: TextStyle,
    /** Supporting sentence under a headline. */
    val body: TextStyle,
    /** Row labels and values in the summary panels. */
    val bodySmall: TextStyle,
    /** Chip labels, the "Change" affordance. */
    val label: TextStyle,
    /** Station detail line, brand sub-line. */
    val caption: TextStyle,
)

val LocalTransitTypography = staticCompositionLocalOf { TransitTypeScheme }

val TransitTheme.type: TransitTypography
    @Composable get() = LocalTransitTypography.current

private fun transitTypography(family: FontFamily) = TransitTypography(
    display = TextStyle(
        fontFamily = family, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp,
    ),
    headline = TextStyle(
        fontFamily = family, fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp, lineHeight = 29.sp, letterSpacing = (-0.5).sp,
    ),
    title = TextStyle(
        fontFamily = family, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = (-0.16).sp,
    ),
    fare = TextStyle(
        fontFamily = family, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp,
    ),
    body = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp,
    ),
    label = TextStyle(
        fontFamily = family, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    caption = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp, lineHeight = 15.sp,
    ),
)

private val TransitColorScheme = TransitColors()
private val TransitTypeScheme = transitTypography(kioskFontFamily())

@Composable
fun TransitTheme(content: @Composable () -> Unit) {
    val colors = TransitColorScheme
    val materialScheme = lightColorScheme(
        primary = colors.primary,
        onPrimary = Color.White,
        primaryContainer = colors.tint,
        onPrimaryContainer = colors.primaryInk,
        secondary = colors.primaryBorder,
        onSecondary = Color.White,
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.surface,
        onSurface = colors.ink,
        surfaceVariant = colors.tint,
        onSurfaceVariant = colors.ink.copy(alpha = 0.6f),
        error = colors.error,
        onError = Color.White,
        errorContainer = colors.errorBackground,
        onErrorContainer = colors.errorInk,
        outline = colors.primaryBorder,
        outlineVariant = colors.border,
    )
    CompositionLocalProvider(
        LocalTransitColors provides colors,
        LocalTransitTypography provides TransitTypeScheme,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography(),
            content = content,
        )
    }
}

package org.multipaz.pos.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class PosColors(
    val surface: Color = Color(0xFF131313),
    val surfaceDim: Color = Color(0xFF131313),
    val surfaceBright: Color = Color(0xFF393939),
    val surfaceContainerLowest: Color = Color(0xFF0E0E0E),
    val surfaceContainerLow: Color = Color(0xFF1C1B1B),
    val surfaceContainer: Color = Color(0xFF201F1F),
    val surfaceContainerHigh: Color = Color(0xFF2A2A2A),
    val surfaceContainerHighest: Color = Color(0xFF353534),
    val surfaceVariant: Color = Color(0xFF353534),
    val onSurface: Color = Color(0xFFE5E2E1),
    val onSurfaceVariant: Color = Color(0xFFD5C4AB),
    val outline: Color = Color(0xFF9E8F78),
    val outlineVariant: Color = Color(0xFF514532),
    val primary: Color = Color(0xFFFFDCA1),
    val onPrimary: Color = Color(0xFF412D00),
    val primaryContainer: Color = Color(0xFFFFB800),
    val onPrimaryContainer: Color = Color(0xFF6B4C00),
    /** The saturated amber used for glows, focus rings and the brand tint. */
    val amber: Color = Color(0xFFFFBA20),
    val secondary: Color = Color(0xFFC8C6C5),
    val onSecondary: Color = Color(0xFF313030),
    val secondaryContainer: Color = Color(0xFF474746),
    val error: Color = Color(0xFFFFB4AB),
    val onError: Color = Color(0xFF690005),
    val errorContainer: Color = Color(0xFF93000A),
    val onErrorContainer: Color = Color(0xFFFFDAD6),
    val success: Color = Color(0xFF22C55E),
)

val LocalPosColors = staticCompositionLocalOf { PosColors() }

/** Convenience accessor: `PosTheme.colors.primary`. */
object PosTheme {
    val colors: PosColors
        @Composable get() = LocalPosColors.current
}

// Hanken Grotesk → clean geometric sans (approximated with the platform sans-serif).
private val Sans = FontFamily.SansSerif

// JetBrains Mono → technical monospace for data / labels (approximated with monospace).
private val Mono = FontFamily.Monospace

@Immutable
data class PosTypography(
    val displayLg: TextStyle = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.96).sp,
    ),
    val headlineLg: TextStyle = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp,
    ),
    val headlineMd: TextStyle = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp,
    ),
    val bodyLg: TextStyle = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 18.sp, lineHeight = 28.sp,
    ),
    val bodyMd: TextStyle = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    val labelMd: TextStyle = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.7.sp,
    ),
    val labelSm: TextStyle = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp,
    ),
    /** Big monospace numerals for the amount readout. */
    val amountDisplay: TextStyle = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.5).sp,
    ),
    /** Monospace numerals for the numpad keys. */
    val numpadKey: TextStyle = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp,
    ),
)

val LocalPosTypography = staticCompositionLocalOf { PosTypography() }

val PosTheme.type: PosTypography
    @Composable get() = LocalPosTypography.current

private val PosColorScheme = PosColors()
private val PosTypeScheme = PosTypography()

@Composable
fun PosTheme(content: @Composable () -> Unit) {
    val colors = PosColorScheme
    val materialScheme = darkColorScheme(
        primary = colors.primaryContainer,
        onPrimary = colors.onPrimaryContainer,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = colors.onPrimaryContainer,
        secondary = colors.secondary,
        onSecondary = colors.onSecondary,
        background = colors.surface,
        onBackground = colors.onSurface,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.onSurfaceVariant,
        error = colors.error,
        onError = colors.onError,
        errorContainer = colors.errorContainer,
        onErrorContainer = colors.onErrorContainer,
        outline = colors.outline,
        outlineVariant = colors.outlineVariant,
    )
    CompositionLocalProvider(
        LocalPosColors provides colors,
        LocalPosTypography provides PosTypeScheme,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography(),
            content = content,
        )
    }
}

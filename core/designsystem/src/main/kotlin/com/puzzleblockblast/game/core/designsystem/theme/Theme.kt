package com.puzzleblockblast.game.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Indigo700,
    onPrimary = Snow,
    primaryContainer = Indigo100,
    onPrimaryContainer = Ink,
    secondary = Coral500,
    onSecondary = Ink,
    tertiary = Lagoon500,
    onTertiary = Snow,
    background = Indigo50,
    onBackground = Ink,
    surface = Snow,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Indigo100,
    onPrimary = Indigo900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo50,
    secondary = Coral200,
    onSecondary = Ink,
    tertiary = Lagoon200,
    onTertiary = Ink,
    background = Indigo900,
    onBackground = Indigo50,
    surface = Indigo800,
    onSurface = Indigo50,
)

private val BaseTypography = Typography()

/** Uses the platform font (scales with the user's font size) with heavier display weights. */
private val PbbTypography = BaseTypography.copy(
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun PbbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBoardColors provides DefaultBoardColors,
        LocalBackgroundGradient provides if (darkTheme) DarkGradient else LightGradient,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = PbbTypography,
            content = content,
        )
    }
}

object PbbTheme {
    val boardColors: BoardColors
        @Composable @ReadOnlyComposable get() = LocalBoardColors.current

    val backgroundGradient: List<Color>
        @Composable @ReadOnlyComposable get() = LocalBackgroundGradient.current
}

/** True when the player turned on "Reduced motion": decorative animations are skipped. */
val LocalReducedMotion = staticCompositionLocalOf { false }

private val LightGradient = listOf(Indigo50, Indigo100, Color(0xFFD9D4F7))
private val DarkGradient = listOf(Color(0xFF2E2470), Indigo900, Color(0xFF0F0D26))
private val LocalBackgroundGradient = staticCompositionLocalOf { LightGradient }

package com.puzzleblockblast.game.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
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
    CompositionLocalProvider(LocalBoardColors provides DefaultBoardColors) {
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
}

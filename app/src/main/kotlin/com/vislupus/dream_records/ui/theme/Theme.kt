package com.vislupus.dream_records.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.vislupus.dream_records.config.AppConfig

/**
 * Maps AppConfig.Theme / Typography / Layout onto Material3. Nothing here is a
 * design decision — change values in AppConfig instead.
 */
private val DarkScheme = darkColorScheme(
    primary = AppConfig.Theme.accent,
    onPrimary = AppConfig.Theme.onAccent,
    background = AppConfig.Theme.background,
    onBackground = AppConfig.Theme.onBackground,
    surface = AppConfig.Theme.surface,
    onSurface = AppConfig.Theme.onSurface,
    surfaceVariant = AppConfig.Theme.surfaceElevated,
    onSurfaceVariant = AppConfig.Theme.onSurfaceMuted,
    outline = AppConfig.Theme.surfaceBorder,
    error = AppConfig.Theme.danger,
    onError = AppConfig.Theme.onAccent,
    secondaryContainer = AppConfig.Theme.surfaceElevated,
    onSecondaryContainer = AppConfig.Theme.onSurface,
    surfaceContainer = AppConfig.Theme.surface,
    surfaceContainerHigh = AppConfig.Theme.surfaceElevated,
    surfaceContainerHighest = AppConfig.Theme.surfaceElevated,
)

/** System sans-serif on Android == Roboto. */
private val AppFont = FontFamily.SansSerif

private val AppTypography = Typography(
    titleLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = AppConfig.Typography.titleSize),
    titleMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = AppConfig.Typography.subtitleSize),
    bodyLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = AppConfig.Typography.bodySize),
    bodyMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = AppConfig.Typography.captionSize),
    bodySmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = AppConfig.Typography.labelSize),
    labelLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = AppConfig.Typography.bodySize),
    labelMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = AppConfig.Typography.captionSize),
    labelSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = AppConfig.Typography.labelSize),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppConfig.Layout.chipCornerRadius),
    small = RoundedCornerShape(AppConfig.Layout.chipCornerRadius),
    medium = RoundedCornerShape(AppConfig.Layout.cornerRadius),
    large = RoundedCornerShape(AppConfig.Layout.cornerRadius),
    extraLarge = RoundedCornerShape(AppConfig.Layout.cornerRadius),
)

@Composable
fun DreamRecordsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

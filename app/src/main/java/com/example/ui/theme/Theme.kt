package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SproutLightColorScheme = lightColorScheme(
    primary = SproutPrimary,
    onPrimary = Color.White,
    primaryContainer = SproutContainer,
    onPrimaryContainer = SproutSecondary,
    secondary = SproutPrimaryBright,
    onSecondary = Color.White,
    secondaryContainer = SproutSurfaceVariant,
    onSecondaryContainer = SproutSecondary,
    tertiary = SproutEmerald,
    onTertiary = Color.White,
    background = SproutLightBg,
    onBackground = SproutTextPrimary,
    surface = SproutSurface,
    onSurface = SproutTextPrimary,
    surfaceVariant = SproutSurfaceVariant,
    onSurfaceVariant = SproutTextSecondary,
    outline = SproutBorder,
    outlineVariant = SproutBorder.copy(alpha = 0.5f),
    error = SproutRose,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = CyanGlow,
    secondary = TealSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = EmeraldSuccess,
    background = CyberDarkBg,
    onBackground = TextPrimary,
    surface = CyberSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorderDark,
    error = RoseError
)

@Composable
fun ParadimAgentTheme(
    isSproutTheme: Boolean = true, // Sprout Green Light UI theme default
    content: @Composable () -> Unit
) {
    val colorScheme = if (isSproutTheme) SproutLightColorScheme else DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

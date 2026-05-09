package com.azhar.aillmgallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo10,
    primaryContainer = Indigo20,
    onPrimaryContainer = Indigo80,
    secondary = Cyan80,
    onSecondary = Cyan10,
    secondaryContainer = Cyan20,
    onSecondaryContainer = Cyan80,
    tertiary = Amber80,
    onTertiary = Amber10,
    tertiaryContainer = Amber20,
    onTertiaryContainer = Amber80,
    error = Error80,
    onError = Neutral10,
    background = Neutral05,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral80,
    outline = Neutral30
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Neutral99,
    primaryContainer = Indigo80,
    onPrimaryContainer = Indigo10,
    secondary = Cyan40,
    onSecondary = Neutral99,
    secondaryContainer = Cyan80,
    onSecondaryContainer = Cyan10,
    tertiary = Amber40,
    onTertiary = Neutral99,
    tertiaryContainer = Amber80,
    onTertiaryContainer = Amber10,
    error = Error40,
    onError = Neutral99,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral95,
    onSurface = Neutral10,
    surfaceVariant = Neutral90,
    onSurfaceVariant = Neutral30,
    outline = Neutral80
)

@Composable
fun AILLMGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
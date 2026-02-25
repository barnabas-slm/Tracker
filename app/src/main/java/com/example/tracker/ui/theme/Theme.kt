package com.example.tracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary   = Purple80,
    secondary = PurpleGrey80,
    tertiary  = Pink80,
    background           = Color(0xFF121212),
    surface              = Color(0xFF1E1E1E),
    surfaceVariant       = Color(0xFF2C2C2C),
    onBackground         = Color(0xFFE6E6E6),
    onSurface            = Color(0xFFE6E6E6),
    onSurfaceVariant     = Color(0xFFB0B0B0),
)

private val LightColorScheme = lightColorScheme(
    primary   = Purple40,
    secondary = PurpleGrey40,
    tertiary  = Pink40,
    background           = Color(0xFFFFFFFF),
    surface              = Color(0xFFFFFFFF),
    surfaceVariant       = Color(0xFFF5F5F5),
    onBackground         = Color(0xFF1C1B1F),
    onSurface            = Color(0xFF1C1B1F),
    onSurfaceVariant     = Color(0xFF49454F),
)

@Composable
fun TrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
package com.kelexine.azubimark.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kelexine.azubimark.data.model.ThemeType
import com.kelexine.azubimark.domain.theme.ThemeManager

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

/**
 * AzubiMark theme composable that integrates with ThemeManager.
 * 
 * This composable observes the ThemeManager state and applies the appropriate
 * Material 3 color scheme, typography, and shapes based on user preferences.
 * 
 * Supports Material You dynamic colors on Android 12+ (API 31+).
 */
@Composable
fun AzubiMarkTheme(
    themeManager: ThemeManager? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDarkTheme = isSystemInDarkTheme()
    
    // If ThemeManager is provided, use it; otherwise fall back to system theme
    val appTheme by (themeManager?.currentTheme?.collectAsState())
        ?: run {
            // Fallback when ThemeManager is not available (e.g., in previews)
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (systemInDarkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                systemInDarkTheme -> DarkColorScheme
                else -> LightColorScheme
            }
            return@run androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(
                    com.kelexine.azubimark.data.model.AppTheme(
                        type = ThemeType.SYSTEM,
                        isDynamicColors = true,
                        colorScheme = colorScheme
                    )
                )
            }
        }
    
    val colorScheme = appTheme.colorScheme
    val isDarkTheme = when (appTheme.type) {
        ThemeType.LIGHT -> false
        ThemeType.DARK -> true
        ThemeType.SYSTEM -> systemInDarkTheme
    }
    
    // Update system bars
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/**
 * Standalone version of AzubiMarkTheme for use without ThemeManager.
 * Useful for previews and testing.
 */
@Composable
fun AzubiMarkTheme(
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

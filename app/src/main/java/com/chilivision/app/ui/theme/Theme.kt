package com.chilivision.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentRed,
    onPrimary = OnSurfacePrimary,
    background = SurfaceBg,
    onBackground = OnSurfacePrimary,
    surface = SurfaceRaised,
    onSurface = OnSurfacePrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = OnSurfaceMuted,
    outline = BorderHairline
)

@Composable
fun CapsibusTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceBg.toArgb()
            window.navigationBarColor = SurfaceBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

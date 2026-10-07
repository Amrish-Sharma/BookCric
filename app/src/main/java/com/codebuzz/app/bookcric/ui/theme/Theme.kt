package com.codebuzz.app.bookcric.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// The match is always played under lights: one dark scheme, whatever the system theme.
private val StadiumColorScheme = darkColorScheme(
    primary = Floodlight,
    onPrimary = OnFloodlight,
    primaryContainer = FloodlightContainer,
    onPrimaryContainer = OnFloodlightContainer,
    secondary = Turf,
    onSecondary = OnTurf,
    secondaryContainer = TurfContainer,
    onSecondaryContainer = OnTurfContainer,
    tertiary = Twilight,
    onTertiary = OnTwilight,
    tertiaryContainer = TwilightContainer,
    onTertiaryContainer = OnTwilightContainer,
    error = Cherry,
    onError = OnCherry,
    errorContainer = CherryContainer,
    onErrorContainer = OnCherryContainer,
    background = NightSky,
    onBackground = Chalk,
    surface = NightSky,
    onSurface = Chalk,
    surfaceVariant = PavilionHigh,
    onSurfaceVariant = ChalkDim,
    surfaceContainerLowest = NightSky,
    surfaceContainerLow = Pavilion,
    surfaceContainer = Pavilion,
    surfaceContainerHigh = PavilionHigh,
    surfaceContainerHighest = PavilionHigh,
    outline = BoundaryRope,
    outlineVariant = BoundaryRopeDim
)

@Composable
fun BookCricketTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = false
            insets.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = StadiumColorScheme,
        typography = Typography,
        content = content
    )
}

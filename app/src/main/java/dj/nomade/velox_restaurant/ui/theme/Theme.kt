package dj.nomade.velox_restaurant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OnyxDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = OnNeonGreen,
    primaryContainer = NeonGreen,
    onPrimaryContainer = OnNeonGreen,

    secondary = SurfaceL3,
    onSecondary = OnSurface,

    background = SurfaceBase,
    onBackground = OnSurface,

    surface = SurfaceL1,
    onSurface = OnSurface,
    surfaceVariant = SurfaceL2,
    onSurfaceVariant = OnSurfaceVar,
    surfaceContainer = SurfaceL2,
    surfaceContainerHigh = SurfaceL3,
    surfaceContainerLow = SurfaceL1,
    surfaceContainerLowest = SurfaceBase,

    outline = OutlineVariant,
    outlineVariant = OutlineVariant,

    error = ErrorColor,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = OnSurface,
    inverseOnSurface = SurfaceBase,
    inversePrimary = OnNeonGreen,

    scrim = Color(0x99000000),
)

@Composable
fun NomadeRestaurantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OnyxDarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}

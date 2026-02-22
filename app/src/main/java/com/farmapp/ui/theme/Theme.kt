package com.farmapp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Farm-friendly earthy color palette
val GreenPrimary = Color(0xFF2E7D32)       // Deep forest green
val GreenLight = Color(0xFF60AD5E)
val GreenContainer = Color(0xFFA5D6A7)
val GreenOnContainer = Color(0xFF003909)

val AmberAccent = Color(0xFFFF8F00)        // Harvest amber
val AmberLight = Color(0xFFFFB300)
val AmberContainer = Color(0xFFFFE082)

val BrownSurface = Color(0xFF4E342E)       // Rich soil brown
val CreamBackground = Color(0xFFF9F6F0)

val RedAlert = Color(0xFFC62828)
val OrangeWarn = Color(0xFFE65100)

val ChickenYellow = Color(0xFFFDD835)
val CropGreen = Color(0xFF33691E)
val MoneyGreen = Color(0xFF1B5E20)
val SkyBlue = Color(0xFF0277BD)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenOnContainer,
    secondary = AmberAccent,
    onSecondary = Color.Black,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Color(0xFF3E2000),
    tertiary = BrownSurface,
    onTertiary = Color.White,
    background = CreamBackground,
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF44474E),
    error = RedAlert,
    onError = Color.White
)

@Composable
fun FarmAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}

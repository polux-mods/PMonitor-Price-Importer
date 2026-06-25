package ua.vitaliyshkarupa.pmonitorimporter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B87),
    secondary = Color(0xFF64CCC5),
    tertiary = Color(0xFFFFB703),
    background = Color(0xFFF6FAFB),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF64CCC5),
    secondary = Color(0xFF176B87),
    tertiary = Color(0xFFFFB703),
    background = Color(0xFF071B22),
    surface = Color(0xFF0D2730),
    onPrimary = Color(0xFF042028)
)

@Composable
fun PMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

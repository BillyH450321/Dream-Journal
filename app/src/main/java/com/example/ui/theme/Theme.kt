package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DreamPurple,
    onPrimary = CosmicBackground,
    secondary = DreamTeal,
    onSecondary = CosmicBackground,
    tertiary = DreamGold,
    background = CosmicBackground,
    surface = EtherealCard,
    onBackground = NebulaLavender,
    onSurface = NebulaLavender,
    outline = EtherealCardBorder,
    surfaceVariant = EtherealCard,
    onSurfaceVariant = NebulaLavender
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DeepVioletAccent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = DreamPurple,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = DreamGold,
    background = LightSurface,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    outline = TextSecondary.copy(alpha = 0.5f)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

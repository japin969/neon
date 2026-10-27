package app.neon.com.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val IosDarkColorScheme = darkColorScheme(
    primary = IosBlueDark,
    onPrimary = IosTextPrimaryDark,
    primaryContainer = IosBlueDark.copy(alpha = 0.2f),
    onPrimaryContainer = IosBlueDark,
    secondary = IosGray,
    onSecondary = IosTextPrimaryDark,
    background = IosBackgroundDark,
    onBackground = IosTextPrimaryDark,
    surface = IosSurfaceDark,
    onSurface = IosTextPrimaryDark,
    surfaceVariant = IosCardDark,
    onSurfaceVariant = IosTextSecondaryDark,
    outline = IosSeparatorDark
)

private val IosLightColorScheme = lightColorScheme(
    primary = IosBlue,
    onPrimary = IosBackgroundLight,
    primaryContainer = IosBlue.copy(alpha = 0.12f),
    onPrimaryContainer = IosBlue,
    secondary = IosGray,
    onSecondary = IosBackgroundLight,
    background = IosBackgroundLight,
    onBackground = IosTextPrimaryLight,
    surface = IosSurfaceLight,
    onSurface = IosTextPrimaryLight,
    surfaceVariant = IosCardLight,
    onSurfaceVariant = IosTextSecondaryLight,
    outline = IosSeparatorLight
)

@Composable
fun NeontTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IosDarkColorScheme else IosLightColorScheme

    val customColors = if (darkTheme) {
        IosCustomColors(
            primary = IosBlueDark,
            secondary = IosGray,
            background = IosBackgroundDark,
            surface = IosSurfaceDark,
            card = IosCardDark,
            textPrimary = IosTextPrimaryDark,
            textSecondary = IosTextSecondaryDark,
            separator = IosSeparatorDark,
            searchBarFill = IosFillDark,
            chatBubbleSent = IosBlueDark,
            chatBubbleReceived = IosChatBubbleReceivedDark,
            accentGreen = IosGreen,
            accentRed = IosRed
        )
    } else {
        IosCustomColors(
            primary = IosBlue,
            secondary = IosGray,
            background = IosBackgroundLight,
            surface = IosSurfaceLight,
            card = IosCardLight,
            textPrimary = IosTextPrimaryLight,
            textSecondary = IosTextSecondaryLight,
            separator = IosSeparatorLight,
            searchBarFill = IosFillLight,
            chatBubbleSent = IosBlue,
            chatBubbleReceived = IosChatBubbleReceivedLight,
            accentGreen = IosGreen,
            accentRed = IosRed
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalIosColors provides customColors,
        LocalIosTypography provides IosSpecificTypography()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = IosTypography,
            content = content
        )
    }
}

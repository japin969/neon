package app.neon.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import app.neon.com.ui.navigation.NeontNavGraph
import dagger.hilt.android.AndroidEntryPoint

// Neon Dark & Light Color Palette
private val NeonGreen = Color(0xFF00FF66)
private val NeonPink = Color(0xFFFF007F)
private val NeonCyan = Color(0xFF00E5FF)
private val DarkBackground = Color(0xFF0D0E15)
private val DarkSurface = Color(0xFF161824)
private val DarkSurfaceVariant = Color(0xFF202334)
private val TextWhite = Color(0xFFF0F2F8)
private val TextGray = Color(0xFF9094A6)

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color.Black,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextGray
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00B347),
    onPrimary = Color.White,
    secondary = Color(0xFF0098A6),
    onSecondary = Color.White,
    tertiary = Color(0xFFD80064),
    onTertiary = Color.White,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF12141B),
    surface = Color.White,
    onSurface = Color(0xFF12141B),
    surfaceVariant = Color(0xFFECEEF4),
    onSurfaceVariant = Color(0xFF5D6377)
)

@Composable
fun NeontTheme(
    darkTheme: Boolean = true, // Default to futuristic dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NeontTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NeontNavGraph(navController = navController)
                }
            }
        }
    }
}

package app.neon.com.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Immutable
internal data class NeontChatPalette(
    val listBackground: Color,
    val detailBackground: Color,
    val bubbleOutgoing: Color,
    val bubbleIncoming: Color,
    val primary: Color,
    val primaryDark: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val timestamp: Color,
    val divider: Color,
    val surface: Color,
    val searchBar: Color,
    val composerBackground: Color,
    val destructive: Color,
    val glassSurfaceLight: Color,
    val glassSurfaceDark: Color,
    val glassBorder: Color,
    val wallpaperBase: Color
)

private val LightChatPalette = NeontChatPalette(
    listBackground = Color(0xFFF2F2F7),
    detailBackground = Color(0xFFEFEAE2),
    bubbleOutgoing = Color(0xFFDCF8C6),
    bubbleIncoming = Color(0xFFFFFFFF),
    primary = Color(0xFF25D366),
    primaryDark = Color(0xFF128C7E),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF8E8E93),
    timestamp = Color(0xFF667781),
    divider = Color(0xFFE5E5EA),
    surface = Color(0xFFFFFFFF),
    searchBar = Color(0xFFE5E5EA),
    composerBackground = Color(0xFFF0F0F0),
    destructive = Color(0xFFFF3B30),
    glassSurfaceLight = Color.White.copy(alpha = 0.72f),
    glassSurfaceDark = Color.Black.copy(alpha = 0.55f),
    glassBorder = Color.White.copy(alpha = 0.35f),
    wallpaperBase = Color(0xFFEFEAE2)
)

private val DarkChatPalette = NeontChatPalette(
    listBackground = Color(0xFF1C1C1E),
    detailBackground = Color(0xFF0B141A),
    bubbleOutgoing = Color(0xFF005C4B),
    bubbleIncoming = Color(0xFF202C33),
    primary = Color(0xFF25D366),
    primaryDark = Color(0xFF128C7E),
    textPrimary = Color(0xFFE9EDEF),
    textSecondary = Color(0xFF8E8E93),
    timestamp = Color(0xFF667781),
    divider = Color(0xFF38383A),
    surface = Color(0xFF2C2C2E),
    searchBar = Color(0xFF2C2C2E),
    composerBackground = Color(0xFF1F2C34),
    destructive = Color(0xFFFF453A),
    glassSurfaceLight = Color.White.copy(alpha = 0.72f),
    glassSurfaceDark = Color.Black.copy(alpha = 0.55f),
    glassBorder = Color.White.copy(alpha = 0.35f),
    wallpaperBase = Color(0xFF0B141A)
)

@ReadOnlyComposable
@Composable
internal fun neontChatPalette(): NeontChatPalette =
    if (isSystemInDarkTheme()) DarkChatPalette else LightChatPalette

// ==========================================
// 🫧 LIQUID GLASS (WHATSAPP iOS 26) MODIFIERS
// ==========================================

/**
 * Modifier Glass Surface Liquid Glass:
 * - Haze blur background
 * - Semi-transparent tinted background (0.72f Light / 0.55f Dark)
 * - Highlight border 0.5dp White alpha 0.35
 * - Ambient & Spot Soft Shadow
 */
fun Modifier.liquidGlassSurface(
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(24.dp),
    blurRadius: Dp = 20.dp,
    tintAlpha: Float? = null,
    isDark: Boolean = false,
    hasShadow: Boolean = true,
    hasBorder: Boolean = true
): Modifier {
    val bgAlpha = tintAlpha ?: if (isDark) 0.55f else 0.72f
    val bgColor = if (isDark) Color.Black.copy(alpha = bgAlpha) else Color.White.copy(alpha = bgAlpha)
    val borderColor = Color.White.copy(alpha = 0.35f)

    var mod: Modifier = this
    if (hasShadow) {
        mod = mod.shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.15f),
            spotColor = Color.Black.copy(alpha = 0.20f)
        )
    }
    mod = mod
        .clip(shape)
        .hazeEffect(state = hazeState) {
            this.blurRadius = blurRadius
        }
        .background(bgColor, shape)

    if (hasBorder) {
        mod = mod.border(0.5.dp, borderColor, shape)
    }
    return mod
}

/**
 * Brush Wallpaper Liquid Glass dengan gradasi radial lembut di atas warna dasar WhatsApp
 */
@Composable
fun chatWallpaperBrush(isDark: Boolean = isSystemInDarkTheme()): Brush {
    val centerColor = if (isDark) Color(0xFF14242D) else Color(0xFFF7F4EE)
    val edgeColor = if (isDark) Color(0xFF0B141A) else Color(0xFFEFEAE2)
    return Brush.radialGradient(
        colors = listOf(centerColor, edgeColor),
        radius = 1800f
    )
}

/**
 * Brush tombol Liquid Glass hijau WhatsApp iOS
 */
fun glassGreenGradientBrush(): Brush = Brush.linearGradient(
    colors = listOf(Color(0xFF25D366), Color(0xFF128C7E))
)

private val FallbackAvatarColors = listOf(
    Color(0xFF0A84FF), Color(0xFFFF9F0A), Color(0xFFFF375F), Color(0xFFBF5AF2),
    Color(0xFF40C8E0), Color(0xFF5E5CE6), Color(0xFF30D158), Color(0xFFFF453A)
)

private fun avatarColorFor(name: String, colorHex: String?): Color {
    val parsed = colorHex?.takeIf { it.isNotBlank() }
        ?.let { hex -> runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() }
    if (parsed != null) return parsed
    return FallbackAvatarColors[Math.floorMod(name.hashCode(), FallbackAvatarColors.size)]
}

private fun firstGlyph(word: String): String = String(Character.toChars(word.codePointAt(0)))

internal fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return "?"
    return words.take(2).joinToString("") { firstGlyph(it) }.uppercase()
}

@Composable
internal fun NeontAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    colorHex: String? = null,
    isOnline: Boolean = false,
    ringColor: Color = neontChatPalette().listBackground
) {
    val background = remember(name, colorHex) { avatarColorFor(name, colorHex) }
    val textColor = if (background.luminance() > 0.6f) Color.Black else Color.White
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(background)
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsOf(name),
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value * 0.38f).sp,
                    lineHeight = TextUnit.Unspecified
                )
            )
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.27f)
                    .background(ringColor, CircleShape)
                    .padding(2.dp)
                    .background(Color(0xFF25D366), CircleShape)
            )
        }
    }
}

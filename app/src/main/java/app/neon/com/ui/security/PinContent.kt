package app.neon.com.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.neon.com.security.PinManager
import app.neon.com.ui.chat.NeontChatPalette
import app.neon.com.ui.chat.neontChatPalette

private val KeypadRows = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9')
)

@Composable
internal fun PinScreenContent(
    title: String,
    subtitle: String?,
    pad: PinPadState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null
) {
    val palette = neontChatPalette()
    val enabled = !pad.isBusy && pad.lockoutSeconds == 0
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(palette.listBackground)) {
        val keySize = if (maxHeight < 640.dp) 60.dp else 72.dp
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, null, tint = palette.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(24.dp))
            PinDots(filled = pad.enteredLength, palette = palette)
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                val message = if (pad.lockoutSeconds > 0) {
                    "Terlalu banyak percobaan. Coba lagi dalam ${formatLockout(pad.lockoutSeconds)}."
                } else pad.errorMessage
                if (message != null) {
                    Text(message, color = palette.destructive, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            PinKeypad(enabled = enabled, keySize = keySize, palette = palette, onDigit = onDigit, onBackspace = onBackspace)
            if (footer != null) { Spacer(modifier = Modifier.height(16.dp)); footer() }
        }
    }
}

@Composable
private fun PinDots(filled: Int, palette: NeontChatPalette) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(PinManager.PIN_LENGTH) { index ->
            val modifier = if (index < filled) {
                Modifier.background(palette.primary, CircleShape)
            } else {
                Modifier.border(1.5.dp, palette.textSecondary, CircleShape)
            }
            Box(modifier = Modifier.size(14.dp).then(modifier))
        }
    }
}

@Composable
private fun PinKeypad(
    enabled: Boolean,
    keySize: Dp,
    palette: NeontChatPalette,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val contentColor = if (enabled) palette.textPrimary else palette.textPrimary.copy(alpha = 0.4f)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        KeypadRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    DigitKey(digit, enabled, keySize, palette, contentColor) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigit(digit)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spacer(modifier = Modifier.size(keySize))
            DigitKey('0', enabled, keySize, palette, contentColor) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigit('0')
            }
            Box(modifier = Modifier.size(keySize).clip(CircleShape).clickable(enabled = enabled) { onBackspace() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.Backspace, "Hapus angka terakhir", tint = contentColor)
            }
        }
    }
}

@Composable
private fun DigitKey(
    digit: Char,
    enabled: Boolean,
    keySize: Dp,
    palette: NeontChatPalette,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(keySize).clip(CircleShape).background(palette.surface).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(digit.toString(), color = contentColor, style = MaterialTheme.typography.displayMedium)
    }
}

private fun formatLockout(seconds: Int): String =
    if (seconds >= 60) "${seconds / 60} menit ${seconds % 60} detik" else "$seconds detik"

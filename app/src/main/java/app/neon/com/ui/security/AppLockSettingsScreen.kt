package app.neon.com.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.neon.com.ui.chat.neontChatPalette

@Composable
fun AppLockSettingsScreen(
    onBackClick: () -> Unit,
    onStartFlow: (PinFlowType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLockSettingsViewModel = hiltViewModel()
) {
    val palette = neontChatPalette()
    val isEnabled by viewModel.isEnabled.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = modifier.fillMaxSize().background(palette.listBackground)
            .verticalScroll(rememberScrollState()).statusBarsPadding()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = palette.textPrimary)
            }
        }
        Text("Kunci Aplikasi", color = palette.textPrimary, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        Text(
            text = if (isEnabled) {
                "Kunci aplikasi aktif. Neont meminta PIN setiap kali dibuka."
            } else {
                "Lindungi Neont dengan PIN 4 digit. PIN diminta setiap kali aplikasi dibuka."
            },
            color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp)).background(palette.surface)
        ) {
            if (!isEnabled) {
                SettingsActionRow(text = "Aktifkan Kunci Aplikasi", color = palette.primary) {
                    onStartFlow(PinFlowType.Enable)
                }
            } else {
                SettingsActionRow(text = "Ubah PIN", color = palette.textPrimary) {
                    onStartFlow(PinFlowType.Change)
                }
                HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                SettingsActionRow(text = "Nonaktifkan Kunci Aplikasi", color = palette.destructive) {
                    onStartFlow(PinFlowType.Disable)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsActionRow(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Text(
        text, color = color, style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

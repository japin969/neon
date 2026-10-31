package app.neon.com.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.neon.com.data.model.User
import app.neon.com.ui.chat.NeontAvatar
import app.neon.com.ui.chat.NeontChatPalette
import app.neon.com.ui.chat.neontChatPalette

private data class SettingsEntry(
    val icon: ImageVector,
    val title: String,
    val tileColor: Color,
    val onClick: () -> Unit
)

@Composable
fun SettingsContent(
    user: User?,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToChatSettings: () -> Unit = {},
    onNavigateToStickers: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {}
) {
    val palette = neontChatPalette()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val accountItems = listOf(
        SettingsEntry(Icons.Default.Notifications, "Notifikasi", Color(0xFFFF3B30), onNavigateToNotifications),
        SettingsEntry(Icons.Default.Lock, "Privasi", Color(0xFF0A84FF), onNavigateToPrivacy),
        SettingsEntry(Icons.Default.Storage, "Data & Storage", Color(0xFF30D158), onNavigateToStorage),
        SettingsEntry(Icons.Default.Settings, "Chat", Color(0xFF25D366), onNavigateToChatSettings)
    )
    val moreItems = listOf(
        SettingsEntry(Icons.Default.EmojiEmotions, "Stiker", Color(0xFFFF9F0A), onNavigateToStickers),
        SettingsEntry(Icons.Default.Devices, "Perangkat", Color(0xFF5E5CE6), onNavigateToDevices),
        SettingsEntry(Icons.Default.Language, "Bahasa", Color(0xFF8E8E93), onNavigateToLanguage)
    )

    Column(
        modifier = modifier.fillMaxSize().background(palette.listBackground).verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = palette.textPrimary)
            }
        }
        Text("Pengaturan", color = palette.textPrimary, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp))

        val displayName = user?.displayName?.ifBlank { user.username }.orEmpty()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp))
                .background(palette.surface).clickable(onClick = onNavigateToEditProfile).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeontAvatar(name = displayName, size = 64.dp, colorHex = user?.avatarColorHex, ringColor = palette.surface)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName.ifBlank { "Memuat..." }, color = palette.textPrimary, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!user?.username.isNullOrBlank()) {
                    Text("@${user?.username}", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.textSecondary)
        }
        Spacer(modifier = Modifier.height(24.dp))
        SettingsGroup(items = accountItems, palette = palette)
        Spacer(modifier = Modifier.height(24.dp))
        SettingsGroup(items = moreItems, palette = palette)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Keluar", color = palette.destructive, style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp))
                .background(palette.surface).clickable { showLogoutDialog = true }.padding(horizontal = 16.dp, vertical = 14.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false }, containerColor = palette.surface,
            title = { Text("Keluar dari akun?") },
            text = { Text("Anda perlu masuk lagi untuk menggunakan Neont.") },
            confirmButton = { TextButton(onClick = { showLogoutDialog = false; onLogoutConfirmed() }) { Text("Keluar", color = palette.destructive) } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun SettingsGroup(items: List<SettingsEntry>, palette: NeontChatPalette) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(palette.surface)) {
        items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth().clickable(onClick = item.onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(item.tileColor), contentAlignment = Alignment.Center) {
                    Icon(item.icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(item.title, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.textSecondary)
            }
            if (index < items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = palette.divider, thickness = 0.5.dp)
            }
        }
    }
}

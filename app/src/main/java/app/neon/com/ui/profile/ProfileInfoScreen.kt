package app.neon.com.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.neon.com.data.model.User
import app.neon.com.ui.chat.NeontAvatar
import app.neon.com.ui.chat.NeontChatPalette
import app.neon.com.ui.chat.neontChatPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ProfileTabs = listOf("Media", "File", "Tautan")

@Composable
fun ProfileInfoScreen(
    user: User,
    onBackClick: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (isVideo: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onBlockClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    mediaContent: @Composable () -> Unit = { EmptyTabMessage("Belum ada media") },
    filesContent: @Composable () -> Unit = { EmptyTabMessage("Belum ada file") },
    linksContent: @Composable () -> Unit = { EmptyTabMessage("Belum ada tautan") }
) {
    val palette = neontChatPalette()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val displayName = user.displayName.ifBlank { user.username }
    val joinedText = remember(user.createdAt) { formatJoinDate(user.createdAt) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.listBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = palette.textPrimary)
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            NeontAvatar(name = displayName, size = 96.dp, colorHex = user.avatarColorHex, ringColor = palette.listBackground)
            Spacer(modifier = Modifier.height(12.dp))
            Text(displayName, color = palette.textPrimary, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            if (user.username.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("@${user.username}", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionTile(Icons.AutoMirrored.Filled.Send, "Pesan", palette, Modifier.weight(1f), onMessageClick)
            ActionTile(Icons.Default.Call, "Telepon", palette, Modifier.weight(1f)) { onCallClick(false) }
            ActionTile(Icons.Default.Videocam, "Video", palette, Modifier.weight(1f)) { onCallClick(true) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp)).background(palette.surface)
        ) {
            InfoRow("Tentang", user.bio.ifBlank { "Tidak ada bio" }, palette)
            if (user.username.isNotBlank()) {
                HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                InfoRow("Username", "@${user.username}", palette)
            }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            InfoRow("Bergabung", joinedText, palette)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp)).background(palette.surface)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ProfileTabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Column(
                        modifier = Modifier.weight(1f).clickable { selectedTab = index }.padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(title, color = if (isSelected) palette.primary else palette.textSecondary, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (isSelected) palette.primary else Color.Transparent))
                    }
                }
            }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            Box(modifier = Modifier.fillMaxWidth()) {
                when (selectedTab) {
                    0 -> mediaContent()
                    1 -> filesContent()
                    else -> linksContent()
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp)).background(palette.surface)
        ) {
            DestructiveRow("Blokir Kontak", palette) { showBlockDialog = true }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            DestructiveRow("Laporkan", palette) { showReportDialog = true }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false }, containerColor = palette.surface,
            title = { Text("Blokir $displayName?") },
            text = { Text("Kontak yang diblokir tidak akan bisa mengirim pesan atau menelepon Anda.") },
            confirmButton = { TextButton(onClick = { showBlockDialog = false; onBlockClick() }) { Text("Blokir", color = palette.destructive) } },
            dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text("Batal") } }
        )
    }
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false }, containerColor = palette.surface,
            title = { Text("Laporkan $displayName?") },
            text = { Text("Laporan akan dikirim untuk ditinjau.") },
            confirmButton = { TextButton(onClick = { showReportDialog = false; onReportClick() }) { Text("Laporkan", color = palette.destructive) } },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, palette: NeontChatPalette, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(palette.surface).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = palette.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = palette.primary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InfoRow(label: String, value: String, palette: NeontChatPalette) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DestructiveRow(text: String, palette: NeontChatPalette, onClick: () -> Unit) {
    Text(
        text, color = palette.destructive, style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
private fun EmptyTabMessage(text: String) {
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = neontChatPalette().textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatJoinDate(timestamp: Long): String =
    if (timestamp <= 0L) "-" else SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(timestamp))

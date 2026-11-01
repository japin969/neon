package app.neon.com.ui.profile

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.User
import app.neon.com.ui.chat.NeontAvatar
import app.neon.com.ui.chat.NeontChatPalette
import app.neon.com.ui.chat.neontChatPalette
import app.neon.com.ui.theme.IosBlue
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
    val context = LocalContext.current

    // Blokir screenshot demi privasi via FLAG_SECURE Activity Lifecycle
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
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
        // Top Nav Back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = IosBlue
                )
            }
        }

        // Header: Avatar Besar 96dp di Tengah, Nama 28sp Bold, Username/Nomor 15sp Abu
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NeontAvatar(
                name = displayName,
                size = 96.dp,
                colorHex = user.avatarColorHex,
                ringColor = palette.listBackground
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = displayName,
                color = palette.textPrimary,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (user.username.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${user.username}",
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Row WhatsApp iOS: 4 Tombol Bulat (Pesan, Telepon, Video, Cari)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ContactActionTile(Icons.AutoMirrored.Filled.Send, "Pesan", palette, Modifier.weight(1f), onMessageClick)
            ContactActionTile(Icons.Default.Call, "Telepon", palette, Modifier.weight(1f)) { onCallClick(false) }
            ContactActionTile(Icons.Default.Videocam, "Video", palette, Modifier.weight(1f)) { onCallClick(true) }
            ContactActionTile(Icons.Default.Search, "Cari", palette, Modifier.weight(1f)) { /* Cari percakapan */ }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Section: Tentang, Username, Bergabung
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
        ) {
            InfoSectionRow("Tentang", user.bio.ifBlank { "Hey there! I am using Neont." }, palette)
            if (user.username.isNotBlank()) {
                HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                InfoSectionRow("Username", "@${user.username}", palette)
            }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            InfoSectionRow("Bergabung", joinedText, palette)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action List Pengaturan Kontak: Bisukan, Pesan sementara, Wallpaper, Simpan
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bisukan Notifikasi",
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isMuted,
                    onCheckedChange = { isMuted = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.primary
                    )
                )
            }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

            RowActionItem(Icons.Default.Timer, "Pesan Sementara", "Mati", palette) { /* Pesan sementara */ }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

            RowActionItem(Icons.Default.Wallpaper, "Wallpaper & Suara", null, palette) { /* Wallpaper */ }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

            RowActionItem(Icons.Default.PersonAdd, "Simpan ke Kontak", null, palette) { /* Simpan kontak */ }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Media, File, Tautan
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ProfileTabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) palette.primary else palette.textSecondary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (isSelected) palette.primary else Color.Transparent)
                        )
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

        // Bagian Aksi Destruktif: Blokir, Laporkan
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
        ) {
            DestructiveButtonRow("Blokir Kontak", palette) { showBlockDialog = true }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            DestructiveButtonRow("Laporkan Kontak", palette) { showReportDialog = true }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            containerColor = palette.surface,
            title = { Text("Blokir $displayName?", color = palette.textPrimary) },
            text = { Text("Kontak yang diblokir tidak akan bisa mengirim pesan atau menelepon Anda di Neont.") },
            confirmButton = {
                TextButton(onClick = {
                    showBlockDialog = false
                    onBlockClick()
                }) {
                    Text("Blokir", color = palette.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = palette.surface,
            title = { Text("Laporkan $displayName?", color = palette.textPrimary) },
            text = { Text("5 pesan terakhir dari kontak ini akan diteruskan ke tim pengawas untuk ditinjau.") },
            confirmButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    onReportClick()
                }) {
                    Text("Laporkan", color = palette.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun ContactActionTile(
    icon: ImageVector,
    label: String,
    palette: NeontChatPalette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(IosBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = IosBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = IosBlue, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
    }
}

@Composable
private fun RowActionItem(
    icon: ImageVector,
    title: String,
    value: String?,
    palette: NeontChatPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (!value.isNullOrBlank()) {
            Text(value, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoSectionRow(label: String, value: String, palette: NeontChatPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(label, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DestructiveButtonRow(text: String, palette: NeontChatPalette, onClick: () -> Unit) {
    Text(
        text = text,
        color = palette.destructive,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
private fun EmptyTabMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = neontChatPalette().textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatJoinDate(timestamp: Long): String =
    if (timestamp <= 0L) "-" else SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(timestamp))
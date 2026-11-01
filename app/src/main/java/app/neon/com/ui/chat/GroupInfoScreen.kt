package app.neon.com.ui.chat

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.User
import app.neon.com.ui.theme.IosBlue

@Composable
fun GroupInfoScreen(
    groupId: String,
    groupName: String,
    groupAvatar: String,
    members: List<User>,
    onBackClick: () -> Unit,
    onAddMemberClick: () -> Unit,
    onLeaveGroupClick: () -> Unit,
    modifier: Modifier = Modifier,
    adminIds: Set<String> = emptySet(),
    isMuted: Boolean = false,
    onMuteChange: (Boolean) -> Unit = {},
    onPromoteClick: (User) -> Unit = {}
) {
    val palette = neontChatPalette()
    val context = LocalContext.current

    // Blokir tangkapan layar (FLAG_SECURE)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    var muted by remember(isMuted) { mutableStateOf(isMuted) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showPromoteDialog by remember { mutableStateOf(false) }

    val admins = remember(members, adminIds) { members.filter { it.uid in adminIds } }
    val promotable = remember(members, adminIds) { members.filter { it.uid !in adminIds } }

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

        // Header: Avatar grup 96dp tengah, nama grup, "X anggota"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NeontAvatar(name = groupName, size = 96.dp, ringColor = palette.listBackground)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = groupName,
                color = palette.textPrimary,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Grup · ${members.size} anggota",
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Row: Pesan, Telepon, Video
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GroupActionTile(Icons.AutoMirrored.Filled.Send, "Pesan", Modifier.weight(1f)) { onBackClick() }
            GroupActionTile(Icons.Default.Call, "Panggilan Suara", Modifier.weight(1f)) { /* Panggilan grup */ }
            GroupActionTile(Icons.Default.Videocam, "Panggilan Video", Modifier.weight(1f)) { /* Video grup */ }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daftar Anggota Horisontal & Tambah Anggota
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
        ) {
            Text(
                text = "Anggota (${members.size})",
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(
                    items = members,
                    key = { index, user -> user.uid.ifBlank { "member_$index" } }
                ) { _, user ->
                    val name = user.displayName.ifBlank { user.username }
                    Column(
                        modifier = Modifier.width(62.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NeontAvatar(
                            name = name,
                            size = 48.dp,
                            colorHex = user.avatarColorHex,
                            ringColor = palette.surface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddMemberClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = palette.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Tambah Anggota", color = palette.primary, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daftar Admin Grup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
        ) {
            Text(
                text = "Admin Grup",
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )

            if (admins.isEmpty()) {
                Text(
                    text = "Belum ada admin lain yang ditugaskan.",
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                admins.forEach { admin ->
                    val name = admin.displayName.ifBlank { admin.username }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeontAvatar(
                            name = name,
                            size = 38.dp,
                            colorHex = admin.avatarColorHex,
                            ringColor = palette.surface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Admin",
                            color = palette.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (promotable.isNotEmpty()) {
                HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                Text(
                    text = "Promosikan Anggota Jadi Admin",
                    color = palette.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPromoteDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Opsi Grup: Bisukan & Pesan Sementara
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
                    checked = muted,
                    onCheckedChange = {
                        muted = it
                        onMuteChange(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.primary
                    )
                )
            }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Opsi pesan sementara */ }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Pesan Sementara", color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("Mati", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Aksi Destruktif: Keluar Grup & Laporkan
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
        ) {
            Text(
                text = "Keluar dari Grup",
                color = palette.destructive,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLeaveDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            Text(
                text = "Laporkan Grup",
                color = palette.destructive,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Laporkan grup */ }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = palette.surface,
            title = { Text("Keluar dari grup?", color = palette.textPrimary) },
            text = { Text("Anda tidak akan lagi menerima pesan dari grup \"$groupName\".") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onLeaveGroupClick()
                }) {
                    Text("Keluar", color = palette.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showPromoteDialog) {
        AlertDialog(
            onDismissRequest = { showPromoteDialog = false },
            containerColor = palette.surface,
            title = { Text("Pilih Anggota", color = palette.textPrimary) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    promotable.forEach { user ->
                        val name = user.displayName.ifBlank { user.username }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPromoteDialog = false
                                    onPromoteClick(user)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeontAvatar(
                                name = name,
                                size = 36.dp,
                                colorHex = user.avatarColorHex,
                                ringColor = palette.surface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPromoteDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun GroupActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(neontChatPalette().surface)
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
        Text(
            text = label,
            color = IosBlue,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
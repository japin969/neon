package app.neon.com.ui.chat

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.neon.com.data.model.User

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
    var muted by remember(isMuted) { mutableStateOf(isMuted) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showPromoteDialog by remember { mutableStateOf(false) }
    val admins = remember(members, adminIds) { members.filter { it.uid in adminIds } }
    val promotable = remember(members, adminIds) { members.filter { it.uid !in adminIds } }

    Column(
        modifier = modifier.fillMaxSize().background(palette.listBackground).verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = palette.textPrimary)
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            NeontAvatar(name = groupName, size = 96.dp, ringColor = palette.listBackground)
            Spacer(modifier = Modifier.height(12.dp))
            Text(groupName, color = palette.textPrimary, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Grup · ${members.size} anggota", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(palette.surface)
        ) {
            Text("Anggota (${members.size})", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(items = members, key = { index, user -> user.uid.ifBlank { "member_$index" } }) { _, user ->
                    val name = user.displayName.ifBlank { user.username }
                    Column(modifier = Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        NeontAvatar(name = name, size = 52.dp, colorHex = user.avatarColorHex, ringColor = palette.surface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(name, color = palette.textPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onAddMemberClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = palette.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Tambah Anggota", color = palette.primary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(palette.surface)
        ) {
            Text("Admin", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
            if (admins.isEmpty()) {
                Text("Belum ada data admin.", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            } else {
                admins.forEach { admin ->
                    val name = admin.displayName.ifBlank { admin.username }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        NeontAvatar(name = name, size = 40.dp, colorHex = admin.avatarColorHex, ringColor = palette.surface)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Admin", color = palette.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (promotable.isNotEmpty()) {
                HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                Text(
                    "Promosikan Anggota", color = palette.primary, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().clickable { showPromoteDialog = true }.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(palette.surface)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Bisukan Notifikasi", color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = muted, onCheckedChange = { muted = it; onMuteChange(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = palette.primary))
            }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            Text(
                "Keluar Grup", color = palette.destructive, style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().clickable { showLeaveDialog = true }.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false }, containerColor = palette.surface,
            title = { Text("Keluar dari grup?") },
            text = { Text("Anda tidak akan menerima pesan dari \"$groupName\" lagi.") },
            confirmButton = { TextButton(onClick = { showLeaveDialog = false; onLeaveGroupClick() }) { Text("Keluar", color = palette.destructive) } },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Batal") } }
        )
    }
    if (showPromoteDialog) {
        AlertDialog(
            onDismissRequest = { showPromoteDialog = false }, containerColor = palette.surface,
            title = { Text("Promosikan menjadi admin") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    promotable.forEach { user ->
                        val name = user.displayName.ifBlank { user.username }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showPromoteDialog = false; onPromoteClick(user) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeontAvatar(name = name, size = 36.dp, colorHex = user.avatarColorHex, ringColor = palette.surface)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPromoteDialog = false }) { Text("Tutup") } }
        )
    }
}

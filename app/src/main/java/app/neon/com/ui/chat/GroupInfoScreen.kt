package app.neon.com.ui.chat

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.User
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.theme.IosBlue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * GroupInfoScreen (WhatsApp iOS 26 Liquid Glass)
 * - FLAG_SECURE: Wajib di Activity Lifecycle
 * - Glass TopBar, Header, Action Row, Member List Glass Card, Action Settings, Destructive
 */
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
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    // FLAG_SECURE: Wajib
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(chatWallpaperBrush(isDark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. TopNav Back (Glass)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .hazeEffect(state = hazeState) {
                        blurRadius = 15.dp
                    }
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.72f)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(0.dp))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = IosBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Info Grup",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Header (Glass Card): Avatar grup 96dp tengah, nama grup, "X Anggota"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(10.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .hazeEffect(state = hazeState) {
                        blurRadius = 20.dp
                    }
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.75f)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .shadow(12.dp, CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                ) {
                    NeontAvatar(name = groupName, size = 96.dp, ringColor = palette.surface)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = groupName,
                    color = palette.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grup · ${members.size} anggota",
                    color = Color(0xFF8E8E93),
                    fontSize = 15.sp,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Action Row (Glass card): Pesan, Telepon, Video
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .hazeEffect(state = hazeState) {
                        blurRadius = 20.dp
                    }
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.75f)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassGroupActionTile(Icons.AutoMirrored.Filled.Send, "Pesan", Modifier.weight(1f)) { onBackClick() }
                GlassGroupActionTile(Icons.Default.Call, "Suara", Modifier.weight(1f)) { /* Panggilan grup */ }
                GlassGroupActionTile(Icons.Default.Videocam, "Video", Modifier.weight(1f)) { /* Video grup */ }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Member List (Glass card, Row padding 12dp, Avatar 40dp, Nama 17sp, Role 13sp #8E8E93)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .hazeEffect(state = hazeState) {
                        blurRadius = 20.dp
                    }
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.75f)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = "Anggota (${members.size})",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp)
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
                                fontSize = 12.sp,
                                fontFamily = InterFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

                // Tambah Anggota
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddMemberClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF25D366))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Tambah Anggota", color = Color(0xFF25D366), fontSize = 17.sp, fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Daftar Admin Grup (Glass card)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .hazeEffect(state = hazeState) {
                        blurRadius = 20.dp
                    }
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.75f)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = "Admin Grup",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp)
                )

                if (admins.isEmpty()) {
                    Text(
                        text = "Belum ada admin lain yang ditugaskan.",
                        color = Color(0xFF8E8E93),
                        fontSize = 15.sp,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    admins.forEach { admin ->
                        val name = admin.displayName.ifBlank { admin.username }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeontAvatar(
                                name = name,
                                size = 40.dp,
                                colorHex = admin.avatarColorHex,
                                ringColor = palette.surface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                color = palette.textPrimary,
                                fontSize = 17.sp,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Admin",
                                color = Color(0xFF25D366),
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (promotable.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                    Text(
                        text = "Promosikan Anggota Jadi Admin",
                        color = Color(0xFF25D366),
                        fontSize = 17.sp,
                        fontFamily = InterFontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPromoteDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Bisukan & Pesan Sementara
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .hazeEffect(state = hazeState) {
                        blurRadius = 20.dp
                    }
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.75f)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bisukan Notifikasi",
                        color = palette.textPrimary,
                        fontSize = 17.sp,
                        fontFamily = InterFontFamily,
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
                            checkedTrackColor = Color(0xFF25D366)
                        )
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Opsi pesan sementara */ }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Pesan Sementara", color = palette.textPrimary, fontSize = 17.sp, fontFamily = InterFontFamily, modifier = Modifier.weight(1f))
                    Text("Mati", color = Color(0xFF8E8E93), fontSize = 15.sp, fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Keluar Grup & Laporkan (Merah)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .hazeEffect(state = hazeState) {
                        blurRadius = 20.dp
                    }
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.75f)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = "Keluar dari Grup",
                    color = Color(0xFFFF3B30),
                    fontSize = 17.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLeaveDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                Text(
                    text = "Laporkan Grup",
                    color = Color(0xFFFF3B30),
                    fontSize = 17.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Laporkan grup */ }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
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
private fun GlassGroupActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(IosBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = IosBlue, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = IosBlue,
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

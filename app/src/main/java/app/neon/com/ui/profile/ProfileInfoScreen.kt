package app.neon.com.ui.profile

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.draw.shadow
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
import app.neon.com.ui.chat.chatWallpaperBrush
import app.neon.com.ui.chat.neontChatPalette
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.theme.IosBlue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ProfileTabs = listOf("Media", "File", "Tautan")

/**
 * ProfileInfoScreen (WhatsApp iOS 26 Liquid Glass)
 * - FLAG_SECURE: Wajib di Activity Lifecycle
 * - Glass TopBar, Header Card, Action Card, Info Card, Destructive Card
 */
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
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    // FLAG_SECURE: Wajib (addFlags di Activity, clear di onDispose)
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
            // 1. TopBar (Glass): Back 24dp #007AFF, Title "Info Kontak" 17sp SemiBold
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
                        text = "Info Kontak",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Header (Glass card, rounded 24dp, padding 16dp): Avatar 96dp (border 2dp White alpha 0.4, shadow), Nama 28sp Bold, Nomor 15sp
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
                    NeontAvatar(
                        name = displayName,
                        size = 96.dp,
                        colorHex = user.avatarColorHex,
                        ringColor = palette.surface
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = displayName,
                    color = palette.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
                if (user.username.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "@${user.username}",
                        color = Color(0xFF8E8E93),
                        fontSize = 15.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Action Row (Glass card, rounded 20dp, padding 16dp): 4 tombol Pesan, Telepon, Video, Cari
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassContactActionTile(Icons.AutoMirrored.Filled.Send, "Pesan", Modifier.weight(1f), onMessageClick)
                GlassContactActionTile(Icons.Default.Call, "Telepon", Modifier.weight(1f)) { onCallClick(false) }
                GlassContactActionTile(Icons.Default.Videocam, "Video", Modifier.weight(1f)) { onCallClick(true) }
                GlassContactActionTile(Icons.Default.Search, "Cari", Modifier.weight(1f)) { /* Cari chat */ }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Info Section (Glass card, rounded 20dp): Label 13sp #8E8E93, Value 17sp
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
                GlassInfoSectionRow("Tentang", user.bio.ifBlank { "Hey there! I am using Neont." }, palette)
                if (user.username.isNotBlank()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                    GlassInfoSectionRow("Username", "@${user.username}", palette)
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                GlassInfoSectionRow("Bergabung", joinedText, palette)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Action List (Glass card, rounded 20dp): Bisukan, Pesan Sementara, Wallpaper, Simpan ke kontak
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
                        checked = isMuted,
                        onCheckedChange = { isMuted = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF25D366)
                        )
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

                GlassRowActionItem(Icons.Default.Timer, "Pesan Sementara", "Mati", palette) { /* Pesan sementara */ }
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

                GlassRowActionItem(Icons.Default.Wallpaper, "Wallpaper & Suara", null, palette) { /* Wallpaper */ }
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

                GlassRowActionItem(Icons.Default.PersonAdd, "Simpan ke Kontak", null, palette) { /* Simpan kontak */ }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Media, File, Tautan (Glass card)
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
                                color = if (isSelected) Color(0xFF25D366) else palette.textSecondary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (isSelected) Color(0xFF25D366) else Color.Transparent)
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                Box(modifier = Modifier.fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> mediaContent()
                        1 -> filesContent()
                        else -> linksContent()
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Destructive (Glass card): Blokir, Laporkan (#FF3B30)
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
                GlassDestructiveButtonRow("Blokir Kontak", palette) { showBlockDialog = true }
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                GlassDestructiveButtonRow("Laporkan Kontak", palette) { showReportDialog = true }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
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
private fun GlassContactActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
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
        Text(label, color = IosBlue, fontSize = 12.sp, fontFamily = InterFontFamily)
    }
}

@Composable
private fun GlassRowActionItem(
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
        Text(title, color = palette.textPrimary, fontSize = 17.sp, fontFamily = InterFontFamily, modifier = Modifier.weight(1f))
        if (!value.isNullOrBlank()) {
            Text(value, color = palette.textSecondary, fontSize = 15.sp, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun GlassInfoSectionRow(label: String, value: String, palette: NeontChatPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(label, color = Color(0xFF8E8E93), fontSize = 13.sp, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = palette.textPrimary, fontSize = 17.sp, fontFamily = InterFontFamily)
    }
}

@Composable
private fun GlassDestructiveButtonRow(text: String, palette: NeontChatPalette, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color(0xFFFF3B30),
        fontSize = 17.sp,
        fontFamily = InterFontFamily,
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
        Text(text, color = Color(0xFF8E8E93), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatJoinDate(timestamp: Long): String =
    if (timestamp <= 0L) "-" else SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(timestamp))

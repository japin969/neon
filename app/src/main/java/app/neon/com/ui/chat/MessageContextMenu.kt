package app.neon.com.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.neon.com.data.model.ChatMessage

val QuickReactionEmojis = listOf("❤️", "👍", "🔥", "😂", "😢", "🙏")

/**
 * Dialog Menu Konteks Pesan dengan 12 Aksi lengkap sesuai standar iOS & WhatsApp.
 */
@Composable
internal fun MessageContextMenu(
    message: ChatMessage,
    currentUserId: String,
    isPinned: Boolean,
    isStarred: Boolean,
    palette: NeontChatPalette,
    onDismiss: () -> Unit,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleStar: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMessageInfo: () -> Unit = {},
    onShare: () -> Unit = {},
    onSaveMedia: () -> Unit = {},
    onSelectMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDeleted = message.isDeleted

    Dialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // 1. Quick Reaction Row (6 Emoji)
            if (!isDeleted) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(palette.surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickReactionEmojis.forEach { emoji ->
                        val isSelected = message.reactions[currentUserId] == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) palette.primary.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { onReaction(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Daftar 12 Aksi Pesan
            Column(
                modifier = Modifier
                    .width(270.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!isDeleted) {
                    // 2. Balas
                    ContextMenuActionRow(Icons.AutoMirrored.Filled.Reply, "Balas", palette.textPrimary, onReply)
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

                    // 3. Salin (Auto-clear clipboard 30 detik)
                    ContextMenuActionRow(Icons.Default.ContentCopy, "Salin", palette.textPrimary) {
                        copyAndScheduleClear(context, message.text)
                        onCopy()
                    }
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

                    // 4. Teruskan
                    ContextMenuActionRow(Icons.Default.Forward, "Teruskan", palette.textPrimary, onForward)
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

                    // 5. Sematkan / Lepas Sematan
                    ContextMenuActionRow(
                        Icons.Default.PushPin,
                        if (isPinned) "Lepas sematan" else "Sematkan",
                        palette.textPrimary,
                        onTogglePin
                    )
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

                    // 6. Bintangi / Hapus Bintang
                    ContextMenuActionRow(
                        Icons.Default.Star,
                        if (isStarred) "Hapus bintang" else "Bintangi",
                        palette.textPrimary,
                        onToggleStar
                    )
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

                    // 7. Edit (hanya pesan outgoing)
                    if (message.isFromMe) {
                        ContextMenuActionRow(Icons.Default.Edit, "Edit", palette.textPrimary, onEdit)
                        HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                    }

                    // 9. Info Pesan
                    ContextMenuActionRow(Icons.Default.Info, "Info pesan", palette.textPrimary, onMessageInfo)
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

                    // 10. Bagikan
                    ContextMenuActionRow(Icons.Default.Share, "Bagikan", palette.textPrimary) {
                        shareTextMessage(context, message.text)
                        onShare()
                    }
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

                    // 11. Simpan Media
                    ContextMenuActionRow(Icons.Default.Download, "Simpan media", palette.textPrimary, onSaveMedia)
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

                    // 12. Pilih (Mode seleksi)
                    ContextMenuActionRow(Icons.Default.CheckCircle, "Pilih", palette.textPrimary, onSelectMode)
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                }

                // 8. Hapus (Destructive)
                ContextMenuActionRow(Icons.Default.Delete, "Hapus", palette.destructive, onDelete)
            }
        }
    }
}

@Composable
private fun ContextMenuActionRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = tint, style = MaterialTheme.typography.bodyLarge)
        Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/**
 * Salin ke clipboard dengan pembersihan otomatis setelah 30 detik demi keamanan data.
 */
fun copyAndScheduleClear(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Neon Message", text))
    Toast.makeText(context, "Teks disalin (dihapus dalam 30 detik)", Toast.LENGTH_SHORT).show()

    Handler(Looper.getMainLooper()).postDelayed({
        runCatching {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }, 30_000L)
}

/**
 * Membuka Share Sheet Android
 */
fun shareTextMessage(context: Context, text: String) {
    if (text.isBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan pesan"))
}
package app.neon.com.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.neon.com.data.model.ChatMessage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

val QuickReactionEmojis = listOf("❤️", "👍", "🔥", "😂", "😢", "🙏")

/**
 * Message Context Menu (Glass Popover WhatsApp iOS 26 Liquid Glass)
 * - Overlay Black alpha 0.25
 * - Width 280dp, shadow 20dp, RoundedCornerShape(28.dp), hazeEffect blur 25dp alpha 0.8, border 0.5dp White alpha 0.35
 * - Quick Reaction Row: Glass pill rounded 24dp, 6 emoji 28sp (❤️ 👍 🔥 😂 😢 🙏), active green gradient
 * - Action List: Column rounded 20dp, glass alpha 0.7, spring anim scaleIn + fadeIn
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
    val isDark = isSystemInDarkTheme()
    val isDeleted = message.isDeleted
    val localHazeState = remember { HazeState() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // A. Overlay: Box fillMaxSize, background Black alpha 0.25, clickable dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Animasi: scaleIn spring (dampingRatio MediumBouncy) + fadeIn 180ms
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 400f
                    )
                ) + fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Menahan klik agar tidak dismiss */ }
                        )
                        .padding(16.dp)
                ) {
                    // C. Quick Reaction Row (Glass pill rounded 24dp, glass alpha 0.5, 6 emoji 28sp)
                    if (!isDeleted) {
                        Row(
                            modifier = Modifier
                                .shadow(12.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .hazeEffect(state = localHazeState) {
                                    blurRadius = 20.dp
                                }
                                .background(
                                    if (isDark) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.70f)
                                )
                                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuickReactionEmojis.forEach { emoji ->
                                val isSelected = message.reactions[currentUserId] == emoji
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) glassGreenGradientBrush() else Color.Transparent
                                        )
                                        .clickable { onReaction(emoji) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 28.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // B. Popover Container: Width 280dp, shadow 20dp, RoundedCornerShape(28.dp), hazeEffect blur 25dp alpha 0.8, border 0.5dp White alpha 0.35
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .shadow(20.dp, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .hazeEffect(state = localHazeState) {
                                blurRadius = 25.dp
                            }
                            .background(
                                if (isDark) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
                            )
                            .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (!isDeleted) {
                            // Balas
                            ContextMenuActionRow(Icons.AutoMirrored.Filled.Reply, "Balas", palette.textPrimary, onReply)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)

                            // Salin (Auto-clear 30 detik)
                            ContextMenuActionRow(Icons.Default.ContentCopy, "Salin", palette.textPrimary) {
                                copyAndScheduleClear(context, message.text)
                                onCopy()
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)

                            // Teruskan
                            ContextMenuActionRow(Icons.Default.Forward, "Teruskan", palette.textPrimary, onForward)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)

                            // Sematkan
                            ContextMenuActionRow(
                                Icons.Default.PushPin,
                                if (isPinned) "Lepas sematan" else "Sematkan",
                                palette.textPrimary,
                                onTogglePin
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)

                            // Bintangi
                            ContextMenuActionRow(
                                Icons.Default.Star,
                                if (isStarred) "Hapus bintang" else "Bintangi",
                                palette.textPrimary,
                                onToggleStar
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)

                            // Edit (hanya isFromMe)
                            if (message.isFromMe) {
                                ContextMenuActionRow(Icons.Default.Edit, "Edit", palette.textPrimary, onEdit)
                                HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)
                            }

                            // Info Pesan
                            ContextMenuActionRow(Icons.Default.Info, "Info pesan", palette.textPrimary, onMessageInfo)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)

                            // Bagikan
                            ContextMenuActionRow(Icons.Default.Share, "Bagikan", palette.textPrimary) {
                                shareTextMessage(context, message.text)
                                onShare()
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)

                            // Simpan Media
                            ContextMenuActionRow(Icons.Default.Download, "Simpan media", palette.textPrimary, onSaveMedia)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)

                            // Pilih
                            ContextMenuActionRow(Icons.Default.CheckCircle, "Pilih", palette.textPrimary, onSelectMode)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)
                        }

                        // Hapus (Merah #FF3B30)
                        ContextMenuActionRow(Icons.Default.Delete, "Hapus", Color(0xFFFF3B30), onDelete)
                    }
                }
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                color = tint,
                fontSize = 17.sp,
                fontFamily = InterFontFamily
            )
        }
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

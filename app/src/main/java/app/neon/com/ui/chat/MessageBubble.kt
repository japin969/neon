package app.neon.com.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.ChatMessage
import app.neon.com.ui.theme.GhAccentGreen
import app.neon.com.ui.theme.GhBorder
import app.neon.com.ui.theme.GhButtonPrimary
import app.neon.com.ui.theme.GhSurface
import app.neon.com.ui.theme.GhTextPrimary
import app.neon.com.ui.theme.GhTextSecondary
import app.neon.com.ui.theme.InterFontFamily

/**
 * MessageBubble - WhatsApp iPhone (iOS) Bubble:
 * - Bubble Keluar: GhButtonPrimary (#1F6FEB), teks putih, sudut 18dp
 * - Bubble Masuk: GhSurface (#161B22), teks GhTextPrimary, sudut 18dp
 * - Long-Press: Membuka popover reaksi iOS (❤️, 👍, 😂, 😮, 😢, 🙏) dan menu Edit/Delete/Forward/Reply
 * - Menampilkan lencana reaksi emoji di sudut bawah bubble
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    onReactionClick: (msgId: String, emoji: String) -> Unit = { _, _ -> },
    onDeleteClick: (msgId: String) -> Unit = {},
    onEditClick: (message: ChatMessage) -> Unit = {},
    onForwardClick: (message: ChatMessage) -> Unit = {},
    onReplyClick: (message: ChatMessage) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    val bubbleShape = remember(message.isFromMe) {
        if (message.isFromMe) {
            RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 4.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 4.dp,
                bottomEnd = 18.dp
            )
        }
    }

    val bubbleColor = if (message.isFromMe) GhButtonPrimary else GhSurface
    val textColor = if (message.isFromMe) Color.White else GhTextPrimary
    val timeColor = if (message.isFromMe) Color.White.copy(alpha = 0.75f) else GhTextSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start
    ) {
        // Popover Reaksi Cepat (Muncul saat Long Press)
        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            QuickReactionRow(
                onSelectEmoji = { emoji ->
                    onReactionClick(message.msgId, emoji)
                    showMenu = false
                }
            )
        }

        Box {
            Surface(
                modifier = Modifier
                    .widthIn(min = 60.dp, max = 290.dp)
                    .clip(bubbleShape)
                    .border(
                        width = 0.5.dp,
                        color = if (message.isFromMe) GhButtonPrimary.copy(alpha = 0.5f) else GhBorder,
                        shape = bubbleShape
                    )
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            if (!message.isDeleted) {
                                showMenu = true
                            }
                        }
                    ),
                shape = bubbleShape,
                color = bubbleColor,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    // Preview Reply jika ada
                    if (!message.replyTo.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (message.isFromMe) Color.Black.copy(alpha = 0.2f) else GhBackground)
                                .border(0.5.dp, GhBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Balasan ke pesan sebelumnya",
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                color = if (message.isFromMe) Color.White.copy(alpha = 0.9f) else GhTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Teks Pesan (Inter 15sp)
                    Text(
                        text = message.text,
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = textColor,
                        fontWeight = if (message.isDeleted) FontWeight.Light else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Baris Waktu & Centang
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isEdited && !message.isDeleted) {
                            Text(
                                text = "diedit ",
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                color = timeColor
                            )
                        }

                        Text(
                            text = message.time,
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = timeColor
                        )

                        if (message.isFromMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = if (message.isRead) "Dibaca" else "Terkirim",
                                tint = if (message.isRead) Color(0xFF64D2FF) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Menu Konteks WhatsApp iOS (Aksi Edit, Hapus, Balas, Teruskan)
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(GhSurface)
                    .border(0.5.dp, GhBorder, RoundedCornerShape(12.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Balas", fontFamily = InterFontFamily, color = GhTextPrimary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = GhButtonPrimary) },
                    onClick = {
                        onReplyClick(message)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Teruskan", fontFamily = InterFontFamily, color = GhTextPrimary) },
                    leadingIcon = { Icon(Icons.Default.Forward, contentDescription = null, tint = GhTextSecondary) },
                    onClick = {
                        onForwardClick(message)
                        showMenu = false
                    }
                )
                if (message.isFromMe && !message.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("Edit Pesan", fontFamily = InterFontFamily, color = GhTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GhButtonPrimary) },
                        onClick = {
                            onEditClick(message)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus Pesan", fontFamily = InterFontFamily, color = Color(0xFFFF453A)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF453A)) },
                        onClick = {
                            onDeleteClick(message.msgId)
                            showMenu = false
                        }
                    )
                }
            }
        }

        // Lencana Reaksi Emoji di Sudut Bubble
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = if (message.isFromMe) 0.dp else 8.dp, end = if (message.isFromMe) 8.dp else 0.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GhSurface)
                    .border(0.5.dp, GhBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val emojiList = message.reactions.values.distinct()
                emojiList.forEach { emoji ->
                    Text(text = emoji, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 2.dp))
                }
                if (message.reactions.size > 1) {
                    Text(
                        text = " ${message.reactions.size}",
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp,
                        color = GhTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Baris Reaksi Cepat iOS WhatsApp (❤️, 👍, 😂, 😮, 😢, 🙏)
 */
@Composable
fun QuickReactionRow(
    onSelectEmoji: (String) -> Unit
) {
    val emojis = listOf("❤️", "👍", "😂", "😮", "😢", "🙏")

    Surface(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .border(0.5.dp, GhBorder, RoundedCornerShape(24.dp)),
        color = GhSurface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            emojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .bouncyClickable { onSelectEmoji(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }
    }
}

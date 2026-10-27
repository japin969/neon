package app.neon.com.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.ChatItem
import app.neon.com.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    recipientId: String,
    isGroup: Boolean,
    chatName: String,
    viewModel: ChatDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (groupId: String) -> Unit = {},
    onStartAudioCall: (targetUserId: String, targetUserName: String) -> Unit,
    onStartVideoCall: (targetUserId: String, targetUserName: String) -> Unit
) {
    LaunchedEffect(chatId) {
        viewModel.initChat(chatId, recipientId, isGroup, chatName)
    }

    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState()

    val darkBg = Color(0xFF0D0E15)
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)

    // Auto-scroll to latest message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP BAR
            ChatDetailTopBar(
                chatName = state.chatName.ifBlank { "Obrolan" },
                isGroup = state.isGroup,
                onNavigateBack = onNavigateBack,
                onHeaderClick = {
                    if (state.isGroup) {
                        onNavigateToGroupInfo(state.chatId)
                    }
                },
                onAudioCall = {
                    val targetId = if (recipientId.isNotBlank()) recipientId else if (state.recipientId.isNotBlank()) state.recipientId else chatId
                    onStartAudioCall(targetId, state.chatName.ifBlank { "Teman Neont" })
                },
                onVideoCall = {
                    val targetId = if (recipientId.isNotBlank()) recipientId else if (state.recipientId.isNotBlank()) state.recipientId else chatId
                    onStartVideoCall(targetId, state.chatName.ifBlank { "Teman Neont" })
                }
            )

            // 2. PINNED MESSAGE HEADER (Jika ada pesan disematkan)
            AnimatedVisibility(
                visible = state.pinnedMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                state.pinnedMessage?.let { pinned ->
                    PinnedMessageHeader(
                        pinnedMessage = pinned,
                        onUnpin = { viewModel.togglePinMessage(pinned) }
                    )
                }
            }

            // 3. MESSAGES LIST
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.isLoading && state.messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = neonGreen)
                    }
                } else if (state.messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Belum ada pesan. Mulai sapa temanmu sekarang!",
                            color = Color(0xFF888EA8),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            val currentUid = state.currentUser?.uid ?: ""
                            val isMe = message.isFromMe(currentUid)

                            SwipeableMessageBubble(
                                message = message,
                                isMe = isMe,
                                onSwipeToReply = {
                                    viewModel.onSwipeReply(message)
                                },
                                onLongClick = {
                                    viewModel.openMessageOptions(message)
                                },
                                onReactionClick = { emoji ->
                                    viewModel.onReactionSelected(message, emoji)
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // 4. REPLY OR EDIT PREVIEW BAR
            AnimatedVisibility(
                visible = state.replyingToMessage != null || state.editingMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ReplyOrEditBanner(
                    replyMessage = state.replyingToMessage,
                    editMessage = state.editingMessage,
                    onCancel = {
                        if (state.editingMessage != null) viewModel.cancelEdit()
                        if (state.replyingToMessage != null) viewModel.cancelReply()
                    }
                )
            }

            // 5. INPUT BAR
            ChatInputBar(
                text = state.inputText,
                isEditing = state.editingMessage != null,
                onTextChange = { viewModel.onInputTextChanged(it) },
                onSend = { viewModel.sendMessage() }
            )
        }

        // ACTION BOTTOM SHEET (Reactions, Reply, Pin, Edit, Forward, Delete)
        if (state.isOptionsMenuVisible && state.selectedMessageForOptions != null) {
            val selected = state.selectedMessageForOptions!!
            val currentUid = state.currentUser?.uid ?: ""
            val isFromMe = selected.isFromMe(currentUid)

            ModalBottomSheet(
                onDismissRequest = { viewModel.closeMessageOptions() },
                sheetState = sheetState,
                containerColor = Color(0xFF161824),
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    // Quick Emoji Reaction Bar (❤️, 👍, 🔥)
                    Text(
                        text = "Berikan Reaksi:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888EA8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF202334))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("❤️", "👍", "🔥").forEach { emoji ->
                            IconButton(onClick = { viewModel.onReactionSelected(selected, emoji) }) {
                                Text(text = emoji, fontSize = 28.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reply Action
                    OptionItemRow(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        title = "Balas Pesan",
                        tint = neonCyan,
                        onClick = {
                            viewModel.onSwipeReply(selected)
                            viewModel.closeMessageOptions()
                        }
                    )

                    // Copy Text Action
                    OptionItemRow(
                        icon = Icons.Default.ContentCopy,
                        title = "Salin Teks",
                        tint = Color.White,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(selected.decryptedText))
                            viewModel.closeMessageOptions()
                        }
                    )

                    // Pin / Unpin Action
                    OptionItemRow(
                        icon = Icons.Default.PushPin,
                        title = if (selected.isPinned) "Lepas Sematan Pesan" else "Sematkan di Header Chat",
                        tint = neonGreen,
                        onClick = { viewModel.togglePinMessage(selected) }
                    )

                    // Edit Action (Only available if sent by current user and not deleted)
                    if (isFromMe && !selected.deletedForEveryone) {
                        OptionItemRow(
                            icon = Icons.Default.Edit,
                            title = "Edit Pesan",
                            tint = Color(0xFFFFB703),
                            onClick = { viewModel.startEditMessage(selected) }
                        )
                    }

                    // Forward Action
                    OptionItemRow(
                        icon = Icons.Default.Forward,
                        title = "Teruskan ke Obrolan Lain",
                        tint = neonCyan,
                        onClick = { viewModel.openForwardDialog(selected) }
                    )

                    // Delete for Me
                    OptionItemRow(
                        icon = Icons.Default.Delete,
                        title = "Hapus untuk Saya",
                        tint = Color(0xFFFF6B6B),
                        onClick = { viewModel.deleteMessageForMe(selected) }
                    )

                    // Delete for Everyone (Only for sender)
                    if (isFromMe && !selected.deletedForEveryone) {
                        OptionItemRow(
                            icon = Icons.Default.Delete,
                            title = "Hapus untuk Semua Orang",
                            tint = Color(0xFFFF0055),
                            onClick = { viewModel.deleteMessageForEveryone(selected) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // FORWARD MESSAGE DIALOG
        if (state.isForwardDialogVisible && state.selectedMessageForOptions != null) {
            ForwardMessageDialog(
                availableChats = state.availableChatsForForward.filter { it.id != state.chatId },
                onDismiss = { viewModel.closeForwardDialog() },
                onSelectChat = { targetChat ->
                    viewModel.forwardMessageToChat(targetChat)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatDetailTopBar(
    chatName: String,
    isGroup: Boolean,
    onNavigateBack: () -> Unit,
    onHeaderClick: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141622))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = Color.White
            )
        }

        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isGroup) neonCyan else neonGreen)
                .combinedClickable(onClick = onHeaderClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chatName.take(2).uppercase(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = onHeaderClick)
        ) {
            Text(
                text = chatName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = neonGreen,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Terenkripsi AES-256",
                    fontSize = 11.sp,
                    color = neonGreen
                )
            }
        }

        // Tombol Panggilan Audio
        IconButton(onClick = onAudioCall) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Panggilan Suara",
                tint = neonCyan
            )
        }

        // Tombol Panggilan Video
        IconButton(onClick = onVideoCall) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Panggilan Video",
                tint = neonGreen
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedMessageHeader(
    pinnedMessage: ChatMessage,
    onUnpin: () -> Unit
) {
    val neonGreen = Color(0xFF00FF66)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181C2C))
            .border(1.dp, neonGreen.copy(alpha = 0.3f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PushPin,
            contentDescription = "Disematkan",
            tint = neonGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pesan Disematkan (${pinnedMessage.senderUsername})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = neonGreen
            )
            Text(
                text = pinnedMessage.decryptedText,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onUnpin, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Lepas Pin",
                tint = Color(0xFF888EA8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    onSwipeToReply: () -> Unit,
    onLongClick: () -> Unit,
    onReactionClick: (String) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val replyTriggerThreshold = 120f

    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)
    val myBubbleBg = Brush.horizontalGradient(listOf(Color(0xFF005F2B), Color(0xFF007A3E)))
    val otherBubbleBg = Color(0xFF1C1F2E)

    val formattedTime = if (message.timestamp > 0) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    } else {
        ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > replyTriggerThreshold) {
                            onSwipeToReply()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // Only allow swiping to the right (positive offset)
                        if (dragAmount > 0 || offsetX > 0) {
                            val newOffset = offsetX + dragAmount
                            offsetX = newOffset.coerceIn(0f, 160f)
                        }
                    }
                )
            }
    ) {
        // Reply indicator revealed when swiping right
        if (offsetX > 20f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(neonGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Balas",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Main Bubble
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth(),
            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .background(if (isMe) myBubbleBg else Brush.linearGradient(listOf(otherBubbleBg, otherBubbleBg)))
                    .border(
                        1.dp,
                        if (isMe) neonGreen.copy(alpha = 0.4f) else Color(0xFF282D42),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
                    .padding(12.dp)
            ) {
                // Forwarded Label
                if (message.forwardedFrom.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward,
                            contentDescription = null,
                            tint = neonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Diteruskan dari ${message.forwardedFrom}",
                            fontSize = 10.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = neonCyan
                        )
                    }
                }

                // Sender Name for other's messages
                if (!isMe) {
                    Text(
                        text = message.senderUsername,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = neonCyan,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Reply Quote Preview inside bubble
                if (message.replyToMessageId.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33000000))
                            .border(1.dp, neonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = message.replyToSender,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = neonGreen
                            )
                            Text(
                                text = message.replyToText,
                                fontSize = 11.sp,
                                color = Color(0xFFD0D4E4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Message Text
                Text(
                    text = message.decryptedText,
                    fontSize = 14.sp,
                    color = Color.White,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp & Status (Edited & Pin badges)
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isEdited) {
                        Text(
                            text = "Edited • ",
                            fontSize = 10.sp,
                            color = Color(0xFFB0B6CE)
                        )
                    }

                    if (message.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = neonGreen,
                            modifier = Modifier.size(12.dp).padding(end = 2.dp)
                        )
                    }

                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = Color(0xFFB0B6CE)
                    )
                }

                // Reaction Pills Row (e.g. ❤️ 2, 👍 1)
                val reactionSummary = message.getReactionSummary()
                if (reactionSummary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        reactionSummary.forEach { (emoji, count) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF141724))
                                    .border(1.dp, Color(0xFF2C324B), RoundedCornerShape(12.dp))
                                    .combinedClickable(onClick = { onReactionClick(emoji) })
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$emoji $count",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReplyOrEditBanner(
    replyMessage: ChatMessage?,
    editMessage: ChatMessage?,
    onCancel: () -> Unit
) {
    val neonGreen = Color(0xFF00FF66)
    val neonPink = Color(0xFFFF007F)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181C2E))
            .border(
                1.dp,
                if (editMessage != null) neonPink.copy(alpha = 0.5f) else neonGreen.copy(alpha = 0.5f)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (editMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = if (editMessage != null) neonPink else neonGreen,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (editMessage != null) "Mengedit Pesan" else "Membalas ke ${replyMessage?.senderUsername}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (editMessage != null) neonPink else neonGreen
            )
            Text(
                text = editMessage?.decryptedText ?: replyMessage?.decryptedText ?: "",
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Batal",
                tint = Color(0xFF888EA8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatInputBar(
    text: String,
    isEditing: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val neonGreen = Color(0xFF00FF66)
    val neonPink = Color(0xFFFF007F)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141622))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    text = if (isEditing) "Edit pesan kamu..." else "Ketik pesan terenkripsi...",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isEditing) neonPink else neonGreen,
                unfocusedBorderColor = Color(0xFF262A3E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = neonGreen
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (text.isNotBlank()) (if (isEditing) neonPink else neonGreen) else Color(0xFF222638))
        ) {
            Icon(
                imageVector = if (isEditing) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                contentDescription = "Kirim",
                tint = if (text.isNotBlank()) Color.Black else Color(0xFF6B7280)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptionItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ForwardMessageDialog(
    availableChats: List<ChatItem>,
    onDismiss: () -> Unit,
    onSelectChat: (ChatItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161824),
        title = {
            Text(
                text = "Teruskan Pesan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            if (availableChats.isEmpty()) {
                Text(
                    text = "Tidak ada obrolan lain yang tersedia.",
                    color = Color(0xFF888EA8),
                    fontSize = 13.sp
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    items(availableChats) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .combinedClickable(onClick = { onSelectChat(chat) })
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FF66)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chat.getDisplayInitials(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = chat.chatName,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = Color(0xFF888EA8))
            }
        }
    )
}

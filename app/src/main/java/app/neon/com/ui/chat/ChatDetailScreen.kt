package app.neon.com.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.ChatMessage
import app.neon.com.ui.theme.GhAccentGreen
import app.neon.com.ui.theme.GhBackground
import app.neon.com.ui.theme.GhBorder
import app.neon.com.ui.theme.GhButtonPrimary
import app.neon.com.ui.theme.GhSurface
import app.neon.com.ui.theme.GhTextPrimary
import app.neon.com.ui.theme.GhTextSecondary
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.viewmodel.ChatViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@Composable
fun ChatDetailScreen(
    chatViewModel: ChatViewModel,
    chatId: String = "chat_aura",
    chatPartnerName: String = "Aura Cantika",
    chatPartnerAvatar: String = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&q=80",
    onBackClick: () -> Unit = {},
    onCallClick: (isVideo: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var currentLimit by remember { mutableIntStateOf(50) }

    var messageToEdit by remember { mutableStateOf<ChatMessage?>(null) }
    var editTextValue by remember { mutableStateOf("") }

    var messageToDeleteId by remember { mutableStateOf<String?>(null) }

    val replyMessage by chatViewModel.replyMessage.collectAsState()
    val allMessages by chatViewModel.messages.collectAsState()

    LaunchedEffect(chatId) {
        chatViewModel.loadChat(chatId)
    }

    val pagedMessages = remember(allMessages, currentLimit) {
        allMessages.takeLast(currentLimit)
    }

    val isUserScrolledUp by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible < total - 2
        }
    }

    val canSend by remember {
        derivedStateOf { inputText.trim().isNotEmpty() }
    }

    LaunchedEffect(allMessages.size) {
        if (!isUserScrolledUp && pagedMessages.isNotEmpty()) {
            listState.animateScrollToItem(pagedMessages.size - 1)
        }
    }

    fun performSendMessage() {
        if (!canSend) return
        val textToSend = inputText.trim()
        chatViewModel.sendMessage(chatId, textToSend)
        inputText = ""

        scope.launch {
            if (pagedMessages.isNotEmpty()) {
                listState.animateScrollToItem(pagedMessages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GhBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GhSurface)
                .border(0.5.dp, GhBorder)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = GhButtonPrimary
                )
            }

            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(chatPartnerAvatar)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = chatPartnerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, GhBorder, CircleShape)
                        .background(GhBackground)
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(GhSurface)
                        .padding(1.5.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(GhAccentGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chatPartnerName,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = GhTextPrimary
                )
                Text(
                    text = "online",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = GhTextSecondary
                )
            }

            IconButton(
                onClick = { onCallClick(true) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = GhButtonPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = { onCallClick(false) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Voice Call",
                    tint = GhButtonPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (allMessages.size > currentLimit) {
                    item(key = "load_more_paging_header") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Muat 50 pesan terdahulu...",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = GhButtonPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GhSurface)
                                    .clickable { currentLimit += 50 }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                items(
                    items = pagedMessages,
                    key = { it.msgId }
                ) { msg ->
                    MessageBubble(
                        message = msg,
                        onReactionClick = { msgId, emoji ->
                            chatViewModel.sendReaction(chatId, msgId, emoji)
                        },
                        onDeleteClick = { msgId ->
                            messageToDeleteId = msgId
                        },
                        onEditClick = { targetMsg ->
                            messageToEdit = targetMsg
                            editTextValue = targetMsg.text
                        },
                        onReplyClick = { targetMsg ->
                            chatViewModel.setReplyMessage(targetMsg)
                        },
                        onForwardClick = { targetMsg ->
                            chatViewModel.sendMessage(chatId, "Forwarded: ${targetMsg.text}")
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = isUserScrolledUp,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(GhSurface)
                        .border(0.5.dp, GhBorder, CircleShape)
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(pagedMessages.size - 1)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to Bottom",
                        tint = GhTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = replyMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            replyMessage?.let { reply ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GhSurface)
                        .border(0.5.dp, GhBorder)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .background(GhButtonPrimary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Membalas pesan",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = GhButtonPrimary
                        )
                        Text(
                            text = reply.text,
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = GhTextPrimary,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { chatViewModel.setReplyMessage(null) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup Balasan",
                            tint = GhTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GhSurface)
                .border(0.5.dp, GhBorder)
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Lampirkan",
                    tint = GhButtonPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GhBackground)
                    .border(0.5.dp, GhBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "iMessage...",
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = GhTextSecondary
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = GhTextPrimary
                    ),
                    cursorBrush = SolidColor(GhButtonPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Kamera",
                    tint = GhTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (canSend) GhButtonPrimary else GhBorder)
                    .clickable { performSendMessage() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim",
                    tint = if (canSend) Color.White else GhTextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }

    if (messageToEdit != null) {
        AlertDialog(
            onDismissRequest = { messageToEdit = null },
            title = {
                Text(
                    text = "Edit Pesan",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GhTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GhBackground,
                        unfocusedContainerColor = GhBackground,
                        focusedBorderColor = GhButtonPrimary,
                        unfocusedBorderColor = GhBorder,
                        focusedTextColor = GhTextPrimary,
                        unfocusedTextColor = GhTextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        messageToEdit?.let { target ->
                            chatViewModel.editMessage(chatId, target.msgId, editTextValue)
                        }
                        messageToEdit = null
                    }
                ) {
                    Text(
                        "Simpan",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = GhButtonPrimary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToEdit = null }) {
                    Text(
                        "Batal",
                        fontFamily = InterFontFamily,
                        color = GhTextSecondary
                    )
                }
            },
            containerColor = GhSurface
        )
    }

    if (messageToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { messageToDeleteId = null },
            title = {
                Text(
                    text = "Hapus Pesan?",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GhTextPrimary
                )
            },
            text = {
                Text(
                    text = "Pesan ini akan dihapus dari server Firebase untuk semua orang.",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = GhTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        messageToDeleteId?.let { id ->
                            chatViewModel.deleteMessage(chatId, id, deleteForEveryone = true)
                        }
                        messageToDeleteId = null
                    }
                ) {
                    Text(
                        "Hapus",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF453A)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDeleteId = null }) {
                    Text(
                        "Batal",
                        fontFamily = InterFontFamily,
                        color = GhTextSecondary
                    )
                }
            },
            containerColor = GhSurface
        )
    }
}

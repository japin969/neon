package app.neon.com.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import app.neon.com.ui.theme.GhButtonPrimary
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

    val replyMessage by chatViewModel.replyMessage.collectAsState()
    val allMessages by chatViewModel.messages.collectAsState()

    LaunchedEffect(chatId) {
        chatViewModel.loadChat(chatId)
    }

    val pagedMessages = remember(allMessages, currentLimit) {
        allMessages.takeLast(currentLimit)
    }

    val canSend by remember {
        derivedStateOf { inputText.trim().isNotEmpty() }
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
            .background(Color(0xFFE5DDD5))
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE5DDD5))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1F2328)
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, Color(0xFFCBD5D1), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(GhAccentGreen)
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chatPartnerName,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1F2328)
                )
                Text(
                    text = "online",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = Color(0xFF667085)
                )
            }

            IconButton(onClick = { onCallClick(true) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video",
                    tint = Color(0xFF1F2328),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = { onCallClick(false) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = Color(0xFF1F2328),
                    modifier = Modifier.size(22.dp)
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
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
                                    .background(Color(0xFFF8FAFC))
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
                    ChatBubbleItem(message = msg)
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
                        .background(Color(0xFFEDF2F7))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .background(GhButtonPrimary, RoundedCornerShape(4.dp))
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
                            fontSize = 12.sp,
                            color = Color(0xFF374151),
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { chatViewModel.setReplyMessage(null) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close reply",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F7F7))
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color(0xFF1F2328),
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(0.5.dp, Color(0xFFD7DCE2), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "Ketik pesan",
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = Color(0xFF98A2B3)
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = Color(0xFF1F2328)
                    ),
                    cursorBrush = SolidColor(GhButtonPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = { }, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color(0xFF1F2328),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) Color(0xFF25D366) else Color(0xFFBFC9D1))
                    .clickable { performSendMessage() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage
) {
    val isOutgoing = message.isFromMe
    val bubbleColor = if (isOutgoing) Color(0xFFDCF8C6) else Color.White
    val textColor = Color(0xFF1F2328)
    val timestampColor = Color(0xFF6B7280)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isOutgoing) Modifier.align(Alignment.CenterEnd) else Modifier.align(Alignment.CenterStart)
                ),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            val bubbleShape = if (isOutgoing) {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
            } else {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp)
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(start = if (isOutgoing) 64.dp else 0.dp, end = if (isOutgoing) 0.dp else 64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(color = bubbleColor, shape = bubbleShape)
                        .border(0.5.dp, Color(0xFFE7E7E7), shape = bubbleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        if (!message.replyTo.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isOutgoing) Color(0xFFBDDEC5) else Color(0xFFF1F5F9))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Balasan",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    color = Color(0xFF374151)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Text(
                            text = message.text,
                            fontFamily = InterFontFamily,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = message.time,
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                color = timestampColor
                            )

                            if (isOutgoing) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Read",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package app.neon.com.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import app.neon.com.data.model.ChatItem
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.theme.IosBlue
import app.neon.com.ui.viewmodel.ChatViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enum kategori filter obrolan
private enum class ChatFilterType(val label: String) {
    ALL("Semua"),
    UNREAD("Belum dibaca"),
    FAVORITES("Favorit"),
    GROUPS("Grup")
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    onChatClick: (chatId: String) -> Unit = {},
    onNavigateToMusic: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onNavigateToNeonAi: () -> Unit = {}
) {
    val palette = neontChatPalette()
    val isDark = isSystemInDarkTheme()
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ChatFilterType.ALL) }
    var showAiDialog by remember { mutableStateOf(false) }

    val now = remember { System.currentTimeMillis() }
    val chatsList = remember {
        mutableStateListOf(
            ChatItem(
                id = "chat_aura",
                chatName = "Aura Cantika",
                lastMessage = "Lagu yang barusan kamu putar di Neont enak banget! 🎶",
                lastMessageTimestamp = now - 20 * 60_000L,
                recipientAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&q=80",
                unreadCount = 2,
                isPinned = true
            ),
            ChatItem(
                id = "chat_komunitas",
                isGroup = true,
                chatName = "Komunitas Musik Jakarta",
                lastMessage = "Diskusi album terbaru Taylor Swift di room musik...",
                lastMessageTimestamp = now - 3 * 3_600_000L,
                recipientAvatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&q=80",
                unreadCount = 0
            ),
            ChatItem(
                id = "chat_rizky",
                chatName = "Rizky Ramadhan",
                lastMessage = "Bro, share playlist Neont kamu dong, buat nemenin kerja.",
                lastMessageTimestamp = now - 26 * 3_600_000L,
                recipientAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80",
                unreadCount = 1
            ),
            ChatItem(
                id = "chat_nadia",
                chatName = "Nadia Salsabila",
                lastMessage = "Makasih ya rekomendasi lagunya! Pas banget di mood.",
                lastMessageTimestamp = now - 30 * 3_600_000L,
                recipientAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80",
                unreadCount = 0
            )
        )
    }

    var selectedContextChat by remember { mutableStateOf<ChatItem?>(null) }

    // Filter daftar percakapan berdasarkan query & tab filter yang aktif
    val filteredChats = remember(searchQuery, selectedFilter, chatsList.toList()) {
        chatsList.filter { chat ->
            val matchQuery = searchQuery.isBlank() ||
                chat.chatName.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessage.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                ChatFilterType.ALL -> true
                ChatFilterType.UNREAD -> chat.unreadCount > 0
                ChatFilterType.FAVORITES -> chat.isPinned
                ChatFilterType.GROUPS -> chat.isGroup
            }
            matchQuery && matchFilter
        }.sortedWith(compareByDescending<ChatItem> { it.isPinned }.thenByDescending { it.lastMessageTimestamp })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chatWallpaperBrush(isDark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ==========================================
            // 1. TOP BAR (LIQUID GLASS)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .hazeEffect(state = hazeState) {
                        blurRadius = 15.dp
                    }
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.72f)
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(0.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Teks "Edit" kiri warna biru iOS #007AFF
                    Text(
                        text = "Edit",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 17.sp,
                        color = IosBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { /* Mode edit bulk */ }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    // Ikon kanan: Security (24dp), Music (24dp), NewChat (24dp), Logout (22dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Keamanan",
                            tint = IosBlue,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { /* Info keamanan */ }
                        )
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Musik",
                            tint = IosBlue,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(onClick = onNavigateToMusic)
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Chat Baru",
                            tint = IosBlue,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { /* Kontak baru */ }
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(onClick = onLogoutClick)
                        )
                    }
                }
            }

            // 2. Large Title "Chats" 34sp Bold
            Text(
                text = "Chats",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 3. Search Bar Liquid Glass (Height 40dp, rounded 12dp, hazeEffect blur 10dp, border 0.5dp White alpha 0.35)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.12f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .hazeEffect(state = hazeState) {
                            blurRadius = 10.dp
                        }
                        .background(
                            if (isDark) Color.Black.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.65f)
                        )
                        .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            color = palette.textPrimary,
                            fontFamily = InterFontFamily
                        ),
                        cursorBrush = SolidColor(IosBlue),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search",
                                            fontSize = 17.sp,
                                            color = palette.textSecondary,
                                            fontFamily = InterFontFamily
                                        )
                                    }
                                    innerTextField()
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus",
                                        tint = palette.textSecondary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // 4. Filter Chips Liquid Glass (LazyRow, Glass Rounded 16dp)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChatFilterType.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    val chipModifier = if (isSelected) {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(glassGreenGradientBrush())
                            .border(0.5.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .hazeEffect(state = hazeState) {
                                blurRadius = 10.dp
                            }
                            .background(
                                if (isDark) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
                            )
                            .border(0.5.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                    }

                    Box(
                        modifier = chipModifier
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            fontFamily = InterFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else palette.textPrimary
                        )
                    }
                }
            }

            // 5. Chat List (LazyColumn, hazeSource agar blur mengambil wallpaper + item)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = hazeState),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filteredChats, key = { it.id }) { chat ->
                        SwipeableChatItem(
                            chat = chat,
                            palette = palette,
                            isDark = isDark,
                            onClick = { onChatClick(chat.id) },
                            onLongClick = { selectedContextChat = chat },
                            onTogglePin = {
                                val idx = chatsList.indexOfFirst { it.id == chat.id }
                                if (idx != -1) {
                                    chatsList[idx] = chat.copy(isPinned = !chat.isPinned)
                                }
                            },
                            onDelete = {
                                chatsList.removeAll { it.id == chat.id }
                            }
                        )
                    }
                }

                // 7. FAB Neon AI (60dp CircleShape, Brush.linearGradient(#25D366, #128C7E), border 0.5dp White alpha 0.4, shadow 12dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(60.dp)
                        .shadow(12.dp, CircleShape, ambientColor = Color(0xFF25D366).copy(alpha = 0.35f))
                        .clip(CircleShape)
                        .background(glassGreenGradientBrush())
                        .border(0.5.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                        .clickable { showAiDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Neon AI",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    // Context Menu Dialog (Long-press) Liquid Glass
    val ctxChat = selectedContextChat
    if (ctxChat != null) {
        ChatListContextMenu(
            chat = ctxChat,
            palette = palette,
            hazeState = hazeState,
            isDark = isDark,
            onDismiss = { selectedContextChat = null },
            onMarkRead = {
                val idx = chatsList.indexOfFirst { it.id == ctxChat.id }
                if (idx != -1) {
                    chatsList[idx] = ctxChat.copy(unreadCount = if (ctxChat.unreadCount > 0) 0 else 1)
                }
                selectedContextChat = null
            },
            onTogglePin = {
                val idx = chatsList.indexOfFirst { it.id == ctxChat.id }
                if (idx != -1) {
                    chatsList[idx] = ctxChat.copy(isPinned = !ctxChat.isPinned)
                }
                selectedContextChat = null
            },
            onMute = {
                selectedContextChat = null
            },
            onArchive = {
                chatsList.removeAll { it.id == ctxChat.id }
                selectedContextChat = null
            },
            onDelete = {
                chatsList.removeAll { it.id == ctxChat.id }
                selectedContextChat = null
            }
        )
    }

    // Dialog Percakapan Penuh Neon AI
    if (showAiDialog) {
        Dialog(
            onDismissRequest = { showAiDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val aiViewModel: ChatViewModel = hiltViewModel()
            ChatDetailScreen(
                chatViewModel = aiViewModel,
                chatId = ChatViewModel.NEON_AI_CHAT_ID,
                chatPartnerName = "Neon AI",
                chatPartnerAvatar = "",
                onBackClick = { showAiDialog = false }
            )
        }
    }
}

/**
 * Item Chat dengan SwipeToDismissBox Material3 + Glass Pin & Delete Background
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableChatItem(
    chat: ChatItem,
    palette: NeontChatPalette,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onTogglePin()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF0A84FF).copy(alpha = 0.85f)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF3B30).copy(alpha = 0.85f)
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                animationSpec = tween(180),
                label = "swipe_bg_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = Color.White)
                } else {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White)
                }
            }
        }
    ) {
        ChatItemView(
            chat = chat,
            palette = palette,
            isDark = isDark,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}

/**
 * Tampilan baris item obrolan: transparan dengan highlight pembatas inset 80dp
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChatItemView(
    chat: ChatItem,
    palette: NeontChatPalette,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar bulat 52dp dengan border 1dp White alpha 0.3
            if (chat.recipientAvatarUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(chat.recipientAvatarUrl)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = chat.chatName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .background(palette.searchBar)
                )
            } else {
                NeontAvatar(
                    name = chat.chatName.ifBlank { "User" },
                    size = 52.dp,
                    colorHex = chat.recipientAvatarColor,
                    ringColor = palette.surface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info obrolan
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.chatName,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (chat.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Disematkan",
                                tint = palette.textSecondary,
                                modifier = Modifier
                                    .size(13.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = formatTimeAgo(chat.lastMessageTimestamp),
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = if (chat.unreadCount > 0) Color(0xFF25D366) else palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        // Unread Badge: glass pill rounded 10dp, Brush.linearGradient(#25D366, #128C7E), text putih 11sp Bold
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(glassGreenGradientBrush())
                                .border(0.5.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Horizontal Divider: 0.5dp White alpha 0.2 (light) / 0.08 (dark), padding start 80dp
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.20f),
            thickness = 0.5.dp
        )
    }
}

/**
 * Context menu saat long-press obrolan (Liquid Glass Surface)
 */
@Composable
private fun ChatListContextMenu(
    chat: ChatItem,
    palette: NeontChatPalette,
    hazeState: HazeState,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
    onTogglePin: () -> Unit,
    onMute: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .shadow(20.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .hazeEffect(state = hazeState) {
                    blurRadius = 25.dp
                }
                .background(
                    if (isDark) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = chat.chatName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onMarkRead)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DoneAll, null, tint = palette.textPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (chat.unreadCount > 0) "Tandai sudah dibaca" else "Tandai belum dibaca",
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTogglePin)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PushPin, null, tint = palette.textPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (chat.isPinned) "Lepas sematan" else "Sematkan",
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onMute)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.NotificationsOff, null, tint = palette.textPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Bisukan", color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onArchive)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Archive, null, tint = palette.textPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Arsipkan", color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Delete, null, tint = palette.destructive, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Hapus chat", color = palette.destructive, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "baru saja"
        diff < 3_600_000L -> "${diff / 60_000L} m"
        diff < 86_400_000L -> "${diff / 3_600_000L} j"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000L} h"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}

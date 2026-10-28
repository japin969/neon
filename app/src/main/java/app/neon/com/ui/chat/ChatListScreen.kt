package app.neon.com.ui.chat

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.ui.theme.GhAccentGreen
import app.neon.com.ui.theme.GhBackground
import app.neon.com.ui.theme.GhBorder
import app.neon.com.ui.theme.GhButtonPrimary
import app.neon.com.ui.theme.GhSurface
import app.neon.com.ui.theme.GhTextPrimary
import app.neon.com.ui.theme.GhTextSecondary
import app.neon.com.ui.theme.InterFontFamily
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Immutable
data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val avatarUrl: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

/**
 * ChatListScreen - WhatsApp iPhone (iOS):
 * - Warna: GhBackground (#0D1117), GhSurface (#161B22), GhBorder (#30363D), GhTextPrimary (#F0F6FC)
 * - Header: "Chats" (Inter Bold 34sp)
 * - Item Chat: Surface RoundedCornerShape(12.dp) + border GhBorder
 * - Nama: Inter SemiBold 17sp
 * - Preview: Inter 15sp GhTextSecondary
 * - LazyColumn WAJIB pakai parameter key unik (Anti-Lag)
 * - Animasi: bouncyClickable
 */
@Composable
fun ChatListScreen(
    onChatClick: (chatId: String) -> Unit = {},
    onNavigateToMusic: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }

    val initialChats = remember {
        listOf(
            ChatItem(
                id = "chat_aura",
                name = "Aura Cantika",
                lastMessage = "Lagu yang barusan kamu putar di Neont enak banget! 🎶",
                time = "10:42",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&q=80",
                unreadCount = 2,
                isOnline = true
            ),
            ChatItem(
                id = "chat_komunitas",
                name = "Komunitas Musik Jakarta",
                lastMessage = "Diskusi album terbaru Taylor Swift di room musik...",
                time = "09:15",
                avatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&q=80",
                unreadCount = 0,
                isOnline = false
            ),
            ChatItem(
                id = "chat_rizky",
                name = "Rizky Ramadhan",
                lastMessage = "Bro, share playlist Neont kamu dong, buat nemenin kerja.",
                time = "Kemarin",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80",
                unreadCount = 1,
                isOnline = true
            ),
            ChatItem(
                id = "chat_nadia",
                name = "Nadia Salsabila",
                lastMessage = "Makasih ya rekomendasi lagunya! Pas banget di mood.",
                time = "Kemarin",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80",
                unreadCount = 0,
                isOnline = false
            )
        )
    }

    val filteredChats = remember(searchQuery, initialChats) {
        if (searchQuery.trim().isEmpty()) {
            initialChats
        } else {
            initialChats.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GhBackground)
            .statusBarsPadding()
    ) {
        // ==========================================
        // 🍎 Header "Chats" (Inter Bold 34sp)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chats",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = 0.37.sp,
                color = GhTextPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tombol Buka Pemutar Musik (ExoPlayer)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GhSurface)
                        .border(0.5.dp, GhBorder, CircleShape)
                        .bouncyClickable(onClick = onNavigateToMusic),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = GhButtonPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tombol Logout Firebase Auth
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GhSurface)
                        .border(0.5.dp, GhBorder, CircleShape)
                        .bouncyClickable(onClick = onLogoutClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = GhTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Search Bar WhatsApp iOS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, GhBorder, RoundedCornerShape(12.dp)),
                placeholder = {
                    Text(
                        text = "Cari percakapan...",
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = GhTextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GhTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = GhTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GhSurface,
                    unfocusedContainerColor = GhSurface,
                    disabledContainerColor = GhSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = GhTextPrimary,
                    unfocusedTextColor = GhTextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ==========================================
        // 💬 LazyColumn WAJIB pakai key unik (Anti-Lag)
        // ==========================================
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                items = filteredChats,
                key = { it.id } // WAJIB KEY UNIK
            ) { chat ->
                ChatConversationItem(
                    chat = chat,
                    onClick = { onChatClick(chat.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Item Chat: Surface RoundedCornerShape(12.dp) + border GhBorder
 */
@Composable
fun ChatConversationItem(
    chat: ChatItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(width = 0.5.dp, color = GhBorder, shape = RoundedCornerShape(12.dp))
            .bouncyClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = GhSurface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Bulat dengan Cache Memory Coil
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(chat.avatarUrl)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = chat.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, GhBorder, CircleShape)
                        .background(GhBackground)
                )

                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(GhSurface)
                            .padding(2.dp)
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
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nama: Inter SemiBold 17sp
                    Text(
                        text = chat.name,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = GhTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = chat.time,
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = if (chat.unreadCount > 0) GhButtonPrimary else GhTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview: Inter 15sp (GhTextSecondary)
                    Text(
                        text = chat.lastMessage,
                        fontFamily = InterFontFamily,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 15.sp,
                        color = if (chat.unreadCount > 0) GhTextPrimary else GhTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(GhButtonPrimary),
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
    }
}

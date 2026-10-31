package app.neon.com.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import app.neon.com.ui.theme.GhButtonPrimary
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.theme.bouncyClickable
import app.neon.com.data.model.ChatItem
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun ChatListScreen(
    onChatClick: (chatId: String) -> Unit = {},
    onNavigateToMusic: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }

    val initialChats = remember {
        val now = System.currentTimeMillis()
        listOf(
            ChatItem(id = "chat_aura", chatName = "Aura Cantika", lastMessage = "Lagu yang barusan kamu putar di Neont enak banget! 🎶", lastMessageTimestamp = now - 20 * 60_000L, recipientAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&q=80", unreadCount = 2),
            ChatItem(id = "chat_komunitas", isGroup = true, chatName = "Komunitas Musik Jakarta", lastMessage = "Diskusi album terbaru Taylor Swift di room musik...", lastMessageTimestamp = now - 3 * 3_600_000L, recipientAvatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&q=80"),
            ChatItem(id = "chat_rizky", chatName = "Rizky Ramadhan", lastMessage = "Bro, share playlist Neont kamu dong, buat nemenin kerja.", lastMessageTimestamp = now - 26 * 3_600_000L, recipientAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80", unreadCount = 1),
            ChatItem(id = "chat_nadia", chatName = "Nadia Salsabila", lastMessage = "Makasih ya rekomendasi lagunya! Pas banget di mood.", lastMessageTimestamp = now - 30 * 3_600_000L, recipientAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80")
        )
    }

    val filteredChats = remember(searchQuery, initialChats) {
        if (searchQuery.isBlank()) initialChats
        else initialChats.filter { it.chatName.contains(searchQuery, true) || it.lastMessage.contains(searchQuery, true) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7)).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFF2F2F7)).padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chats", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = Color(0xFF111827))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFE8EDF2)).border(0.5.dp, Color(0xFFE3E8EF), CircleShape).bouncyClickable(onClick = onNavigateToMusic),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.MusicNote, "Music", tint = Color(0xFF1F2328), modifier = Modifier.size(20.dp)) }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFE8EDF2)).border(0.5.dp, Color(0xFFE3E8EF), CircleShape).bouncyClickable(onClick = onLogoutClick),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = Color(0xFF1F2328), modifier = Modifier.size(18.dp)) }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).border(0.5.dp, Color(0xFFE4E7EC), RoundedCornerShape(12.dp)),
                placeholder = { Text("Cari percakapan...", fontFamily = InterFontFamily, fontSize = 15.sp, color = Color(0xFF98A2B3)) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color(0xFF98A2B3), modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, "Clear", tint = Color(0xFF98A2B3), modifier = Modifier.size(16.dp)) }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827)
                )
            )
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            items(filteredChats, key = { it.id }) { chat ->
                ChatConversationItem(chat = chat, onClick = { onChatClick(chat.id) })
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ChatConversationItem(chat: ChatItem, onClick: () -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(0.5.dp, Color(0xFFE4E7EC), RoundedCornerShape(12.dp)).bouncyClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), color = Color.White, tonalElevation = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(chat.recipientAvatarUrl).crossfade(true).memoryCachePolicy(CachePolicy.ENABLED).build(),
                    contentDescription = chat.chatName, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE8EDF2))
                )
                if (false) {
                    Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(Color.White).padding(1.5.dp).align(Alignment.BottomEnd)) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(GhAccentGreen))
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(chat.chatName, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF1F2328), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatTime(chat.lastMessageTimestamp), fontFamily = InterFontFamily, fontSize = 12.sp, color = if (chat.unreadCount > 0) GhButtonPrimary else Color(0xFF8A8F98))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(chat.lastMessage, fontFamily = InterFontFamily, fontSize = 15.sp, color = if (chat.unreadCount > 0) Color(0xFF1F2328) else Color(0xFF5D6470), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(GhButtonPrimary), contentAlignment = Alignment.Center) {
                            Text(chat.unreadCount.toString(), fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "baru saja"
        diff < 3_600_000L -> "${diff / 60_000L} menit"
        diff < 86_400_000L -> "${diff / 3_600_000L} jam"
        else -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}

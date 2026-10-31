package app.neon.com.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.neon.com.data.model.ChatItem

enum class LastMessageTick { Sent, Read }

private val SwipePinColor = Color(0xFF0A84FF)
private val ReadTickColor = Color(0xFF34B7F1)
private val DividerInset = 80.dp
private val StringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() }
)

@Composable
fun ChatListContent(
    chats: List<ChatItem>,
    onChatClick: (ChatItem) -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier,
    lastMessageTick: (ChatItem) -> LastMessageTick? = { null },
    onMoreClick: () -> Unit = {},
    onMarkAsRead: (ChatItem) -> Unit = {},
    onPinChange: (ChatItem, Boolean) -> Unit = { _, _ -> },
    onMuteChange: (ChatItem, Boolean) -> Unit = { _, _ -> },
    onArchiveChange: (ChatItem, Boolean) -> Unit = { _, _ -> },
    onDelete: (ChatItem) -> Unit = {}
) {
    val palette = neontChatPalette()
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var isSearchVisible by rememberSaveable { mutableStateOf(true) }
    var pinnedIds by rememberSaveable(stateSaver = StringSetSaver) { mutableStateOf(emptySet<String>()) }
    var mutedIds by rememberSaveable(stateSaver = StringSetSaver) { mutableStateOf(emptySet<String>()) }
    var archivedIds by rememberSaveable(stateSaver = StringSetSaver) { mutableStateOf(emptySet<String>()) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var menuChatId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<ChatItem?>(null) }

    val showingArchived = showArchived && archivedIds.isNotEmpty()
    val visibleChats = remember(chats, query, pinnedIds, archivedIds, showingArchived) {
        val q = query.trim()
        chats
            .filter { (it.id in archivedIds) == showingArchived }
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) || it.lastMessage.contains(q, ignoreCase = true) }
            .sortedByDescending { it.id in pinnedIds }
    }
    // isi selanjutnya ada di Langkah 3B

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.listBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatListTopBar(
                palette = palette,
                onSearchClick = {
                    isSearchVisible = !isSearchVisible
                    if (!isSearchVisible) { query = ""; focusManager.clearFocus() }
                },
                onMoreClick = onMoreClick
            )
            if (isSearchVisible) {
                ChatSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    palette = palette,
                    onSearch = { focusManager.clearFocus() }
                )
            }
            if (archivedIds.isNotEmpty()) {
                ArchivedToggleRow(
                    count = archivedIds.size,
                    isShowing = showingArchived,
                    palette = palette,
                    onClick = { showArchived = !showingArchived }
                )
            }
            if (visibleChats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            query.isNotBlank() -> "Tidak ada hasil untuk \"${query.trim()}\""
                            showingArchived -> "Tidak ada chat terarsip"
                            else -> "Belum ada percakapan"
                        },
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(visibleChats, key = { it.id }) { chat ->
                        val isPinned = chat.id in pinnedIds
                        val isMuted = chat.id in mutedIds
                        val isArchived = chat.id in archivedIds
                        ChatListRow(
                            chat = chat,
                            isPinned = isPinned,
                            isMuted = isMuted,
                            isArchived = isArchived,
                            tick = lastMessageTick(chat),
                            palette = palette,
                            menuExpanded = menuChatId == chat.id,
                            onClick = { focusManager.clearFocus(); onChatClick(chat) },
                            onLongClick = { menuChatId = chat.id },
                            onDismissMenu = { menuChatId = null },
                            onMarkAsRead = { onMarkAsRead(chat) },
                            onTogglePin = { pinnedIds = if (isPinned) pinnedIds - chat.id else pinnedIds + chat.id; onPinChange(chat, !isPinned) },
                            onToggleMute = { mutedIds = if (isMuted) mutedIds - chat.id else mutedIds + chat.id; onMuteChange(chat, !isMuted) },
                            onToggleArchive = { archivedIds = if (isArchived) archivedIds - chat.id else archivedIds + chat.id; onArchiveChange(chat, !isArchived) },
                            onRequestDelete = { pendingDelete = chat }
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onNewChatClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp),
            shape = CircleShape,
            containerColor = palette.primary,
            contentColor = Color.White
        ) { Icon(imageVector = Icons.Default.Add, contentDescription = "Chat baru") }
    }

    pendingDelete?.let { chat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = palette.surface,
            title = { Text("Hapus percakapan?") },
            text = { Text("Percakapan dengan ${chat.chatName} akan dihapus dari daftar Anda.") },
            confirmButton = {
                TextButton(onClick = { onDelete(chat); pendingDelete = null }) {
                    Text("Hapus", color = palette.destructive)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun ChatListTopBar(
    palette: NeontChatPalette,
    onSearchClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Neont",
            color = palette.textPrimary,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearchClick) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Cari", tint = palette.textPrimary)
        }
        IconButton(onClick = onMoreClick) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Lainnya", tint = palette.textPrimary)
        }
    }
}

@Composable
private fun ChatSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    palette: NeontChatPalette,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .height(36.dp).clip(RoundedCornerShape(10.dp)).background(palette.searchBar)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(text = "Cari", color = palette.textSecondary, style = MaterialTheme.typography.bodyLarge)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                cursorBrush = SolidColor(palette.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hapus pencarian",
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp).clickable { onQueryChange("") }
            )
        }
    }
}

@Composable
private fun ArchivedToggleRow(
    count: Int,
    isShowing: Boolean,
    palette: NeontChatPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.Archive, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (isShowing) "Kembali ke semua chat" else "Diarsipkan ($count)",
            color = palette.textPrimary,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChatListRow(
    chat: ChatItem,
    isPinned: Boolean,
    isMuted: Boolean,
    isArchived: Boolean,
    tick: LastMessageTick?,
    palette: NeontChatPalette,
    menuExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onMarkAsRead: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleArchive: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val latestTogglePin by rememberUpdatedState(onTogglePin)
    val latestRequestDelete by rememberUpdatedState(onRequestDelete)
    val hasUnread = chat.unreadCount > 0
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> latestTogglePin()
                SwipeToDismissBoxValue.EndToStart -> latestRequestDelete()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        }
    )
    Column {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val backgroundColor = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> SwipePinColor
                    SwipeToDismissBoxValue.EndToStart -> palette.destructive
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(backgroundColor).padding(horizontal = 24.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Icon(imageVector = Icons.Default.PushPin, contentDescription = if (isPinned) "Lepas sematan" else "Sematkan", tint = Color.White)
                        SwipeToDismissBoxValue.EndToStart -> Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White)
                        SwipeToDismissBoxValue.Settled -> Unit
                    }
                }
            }
        ) {
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth().background(palette.listBackground)
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeontAvatar(name = chat.chatName, size = 52.dp, isOnline = false, ringColor = palette.listBackground)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = chat.chatName, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = formatTime(chat.lastMessageTimestamp), color = if (hasUnread) palette.primary else palette.timestamp, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (tick != null) {
                                Icon(
                                    imageVector = if (tick == LastMessageTick.Read) Icons.Default.DoneAll else Icons.Default.Done,
                                    contentDescription = null,
                                    tint = if (tick == LastMessageTick.Read) ReadTickColor else palette.timestamp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = chat.lastMessage, color = palette.timestamp, style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            if (isMuted) { Spacer(modifier = Modifier.width(6.dp)); Icon(imageVector = Icons.Default.VolumeOff, contentDescription = "Dibisukan", tint = palette.textSecondary, modifier = Modifier.size(16.dp)) }
                            if (isPinned) { Spacer(modifier = Modifier.width(6.dp)); Icon(imageVector = Icons.Default.PushPin, contentDescription = "Disematkan", tint = palette.textSecondary, modifier = Modifier.size(16.dp)) }
                            if (hasUnread) { Spacer(modifier = Modifier.width(6.dp)); UnreadBadge(count = chat.unreadCount, palette = palette) }
                        }
                    }
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                    if (hasUnread) {
                        DropdownMenuItem(text = { Text("Tandai sudah dibaca") }, onClick = { onDismissMenu(); onMarkAsRead() })
                    }
                    DropdownMenuItem(text = { Text(if (isPinned) "Lepas sematan" else "Sematkan") }, onClick = { onDismissMenu(); onTogglePin() })
                    DropdownMenuItem(text = { Text(if (isMuted) "Aktifkan notifikasi" else "Bisukan") }, onClick = { onDismissMenu(); onToggleMute() })
                    DropdownMenuItem(text = { Text(if (isArchived) "Keluarkan dari arsip" else "Arsipkan") }, onClick = { onDismissMenu(); onToggleArchive() })
                    DropdownMenuItem(text = { Text("Hapus", color = palette.destructive) }, onClick = { onDismissMenu(); onRequestDelete() })
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = DividerInset), thickness = 0.5.dp, color = palette.divider)
    }
}

@Composable
private fun UnreadBadge(count: Int, palette: NeontChatPalette) {
    Box(
        modifier = Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .background(palette.primary, CircleShape).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
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

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.ChatItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onNavigateToChatDetail: (chatId: String, recipientId: String, isGroup: Boolean, chatName: String) -> Unit,
    onNavigateToCreateGroup: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF007F)
    val darkBg = Color(0xFF0D0E15)
    val darkCard = Color(0xFF161824)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR: Search & Action Header
            ChatListTopBar(
                isSearchActive = state.isSearchActive,
                searchQuery = state.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onToggleSearch = { viewModel.toggleSearchActive(it) },
                onOpenAddContact = { viewModel.openAddContactDialog() }
            )

            if (state.isLoading && state.allChats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = neonGreen)
                }
            } else if (state.activeChats.isEmpty() && state.archivedChats.isEmpty()) {
                // Empty State
                ChatListEmptyState(
                    onAddContact = { viewModel.openAddContactDialog() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 1. Folder Arsip Banner (jika ada chat terarsip)
                    if (state.archivedChats.isNotEmpty()) {
                        item {
                            ArchivedFolderBanner(
                                count = state.archivedChats.size,
                                isExpanded = state.isShowingArchivedFolder,
                                onToggle = { viewModel.toggleShowArchivedFolder() }
                            )
                        }

                        if (state.isShowingArchivedFolder) {
                            items(state.archivedChats, key = { "archived_${it.id}" }) { chat ->
                                ChatItemRow(
                                    chat = chat,
                                    onClick = {
                                        onNavigateToChatDetail(chat.id, chat.recipientId, chat.isGroup, chat.chatName)
                                    },
                                    onLongClick = {
                                        viewModel.openActionMenu(chat)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color(0xFF202334)
                                )
                            }
                        }
                    }

                    // 2. Section: Disematkan (Pinned Chats)
                    if (state.pinnedChats.isNotEmpty()) {
                        item {
                            SectionHeader(title = "DISEMATKAN", count = state.pinnedChats.size, tint = neonGreen)
                        }
                        items(state.pinnedChats, key = { "pinned_${it.id}" }) { chat ->
                            ChatItemRow(
                                chat = chat,
                                onClick = {
                                    onNavigateToChatDetail(chat.id, chat.recipientId, chat.isGroup, chat.chatName)
                                },
                                onLongClick = {
                                    viewModel.openActionMenu(chat)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // 3. Section: Semua Obrolan (Unpinned Active Chats)
                    if (state.unpinnedChats.isNotEmpty()) {
                        item {
                            SectionHeader(title = "SEMUA OBROLAN", count = state.unpinnedChats.size, tint = Color(0xFF888EA8))
                        }
                        items(state.unpinnedChats, key = { "unpinned_${it.id}" }) { chat ->
                            ChatItemRow(
                                chat = chat,
                                onClick = {
                                    onNavigateToChatDetail(chat.id, chat.recipientId, chat.isGroup, chat.chatName)
                                },
                                onLongClick = {
                                    viewModel.openActionMenu(chat)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Action Bottom Sheet for Long-Pressed Chat
        if (state.isActionMenuVisible && state.selectedChatForAction != null) {
            val selected = state.selectedChatForAction!!
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeActionMenu() },
                sheetState = sheetState,
                containerColor = darkCard,
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = selected.chatName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Pin / Unpin Action
                    Button(
                        onClick = { viewModel.togglePinChat(selected) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222638)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (selected.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = neonGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (selected.isPinned) "Lepas Sematan (Unpin)" else "Sematkan Obrolan (Pin)",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Archive / Unarchive Action
                    Button(
                        onClick = { viewModel.toggleArchiveChat(selected) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222638)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = neonCyan
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (selected.isArchived) "Keluarkan dari Arsip" else "Arsipkan Obrolan",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delete Chat Action (Soft delete from list)
                    Button(
                        onClick = { viewModel.deleteChatFromList(selected) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF0055)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF4D6D)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Hapus dari Daftar Obrolan",
                            color = Color(0xFFFF4D6D),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Add Contact Dialog
        if (state.isAddContactDialogVisible) {
            AddContactDialog(
                searchQuery = state.contactSearchQuery,
                onQueryChange = { viewModel.onContactSearchQueryChange(it) },
                isSearching = state.isSearchingContact,
                foundContact = state.foundContact,
                searchError = state.contactSearchError,
                onDismiss = { viewModel.closeAddContactDialog() },
                onStartChat = {
                    viewModel.startChatWithFoundContact(onNavigateToChatDetail)
                }
            )
        }
    }
}

@Composable
private fun ChatListTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onOpenAddContact: () -> Unit
) {
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isSearchActive) {
            // Search Input Active
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cari pesan atau @kontak...", color = Color(0xFF6B7280)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = neonGreen
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { onToggleSearch(false) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Pencarian",
                            tint = Color.White
                        )
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = neonGreen,
                    unfocusedBorderColor = Color(0xFF262A3E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = neonGreen
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
        } else {
            // Normal Header: Brand & Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(neonGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEONT CHAT",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tombol Buka Pencarian
                IconButton(
                    onClick = { onToggleSearch(true) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161824))
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tombol Tambah Kontak
                IconButton(
                    onClick = onOpenAddContact,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161824))
                        .border(1.dp, neonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Tambah Kontak",
                        tint = neonCyan
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatItemRow(
    chat: ChatItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val darkCard = Color(0xFF161824)
    val neonGreen = Color(0xFF00FF66)
    val neonPink = Color(0xFFFF007F)

    // Parse avatar color
    val avatarBgColor = try {
        Color(android.graphics.Color.parseColor(chat.recipientAvatarColor))
    } catch (_: Exception) {
        neonGreen
    }

    val formattedTime = if (chat.lastMessageTimestamp > 0) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(chat.lastMessageTimestamp))
    } else {
        ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(darkCard)
            .border(
                width = if (chat.isPinned) 1.5.dp else 1.dp,
                color = if (chat.isPinned) neonGreen.copy(alpha = 0.6f) else Color(0xFF222638),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Circle with Initials
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarBgColor)
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.getDisplayInitials(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center: Name & Last Message
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.chatName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = Color(0xFF7E849B)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage,
                    fontSize = 13.sp,
                    color = Color(0xFF9094A6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = neonGreen,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(neonPink)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedFolderBanner(
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val neonCyan = Color(0xFF00E5FF)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131726))
            .border(1.dp, neonCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = "Folder Arsip",
                tint = neonCyan
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Folder Pesan Terarsip ($count)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = "Toggle Folder",
            tint = neonCyan
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title ($count)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = tint
        )
    }
}

@Composable
private fun ChatListEmptyState(onAddContact: () -> Unit) {
    val neonGreen = Color(0xFF00FF66)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B2332)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = neonGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Belum Ada Obrolan",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Temukan teman dengan @username mereka dan mulai percakapan terenkripsi sekarang.",
            fontSize = 13.sp,
            color = Color(0xFF888EA8),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )

        Button(
            onClick = onAddContact,
            colors = ButtonDefaults.buttonColors(containerColor = neonGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Tambah Kontak", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddContactDialog(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    foundContact: app.neon.com.data.model.User?,
    searchError: String?,
    onDismiss: () -> Unit,
    onStartChat: () -> Unit
) {
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)
    val darkCard = Color(0xFF161824)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = darkCard,
        title = {
            Text(
                text = "Tambah Kontak Baru",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Ketik @username yang terdaftar di Neont:",
                    fontSize = 13.sp,
                    color = Color(0xFF9094A6),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("@username", color = Color(0xFF6B7280)) },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = neonGreen
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = neonGreen,
                        unfocusedBorderColor = Color(0xFF262A3E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = neonGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (searchError != null) {
                    Text(
                        text = searchError,
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B6B),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Hasil Kontak Ditemukan
                if (foundContact != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202334))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatarColor = try {
                            Color(android.graphics.Color.parseColor(foundContact.avatarColorHex))
                        } catch (_: Exception) {
                            neonGreen
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = foundContact.getInitials(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "@${foundContact.username}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = foundContact.bio,
                                fontSize = 12.sp,
                                color = Color(0xFF888EA8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartChat,
                enabled = foundContact != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = neonGreen,
                    disabledContainerColor = Color(0xFF222B26)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Mulai Obrolan",
                    color = if (foundContact != null) Color.Black else Color(0xFF6B7280),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = Color(0xFF888EA8))
            }
        }
    )
}

package app.neon.com.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.neon.com.data.model.ChatItem
import app.neon.com.data.model.ChatMessage
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.theme.IosBlue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val MAX_MESSAGE_LENGTH = 4000
private const val DAY_MS = 24L * 60L * 60L * 1000L
private val ReadTickColor = Color(0xFF34B7F1)
private val DetailStringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() }
)

private sealed interface ChatEntry {
    data class Day(val key: Int, val label: String) : ChatEntry
    data class Msg(val message: ChatMessage) : ChatEntry
}

/**
 * Komponen Stateless Konten Chat Detail dengan 20 parameter (NavGraph contract).
 * Desain: Liquid Glass WhatsApp iOS 26 dengan Haze blur, floating composer, tanpa border bubble.
 */
@Composable
fun ChatDetailContent(
    messages: List<ChatMessage>,
    currentUserId: String,
    chatPartnerName: String,
    onBackClick: () -> Unit,
    onCallClick: (isVideo: Boolean) -> Unit,
    onSendMessage: (text: String, replyTo: String?) -> Unit,
    modifier: Modifier = Modifier,
    isPartnerOnline: Boolean = false,
    statusText: String? = null,
    isGroup: Boolean = false,
    forwardTargets: List<ChatItem> = emptyList(),
    onTopBarClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onMicClick: () -> Unit = {},
    onReaction: (ChatMessage, String) -> Unit = { _, _ -> },
    onEditMessage: (ChatMessage, String) -> Unit = { _, _ -> },
    onDeleteMessage: (ChatMessage, Boolean) -> Unit = { _, _ -> },
    onForwardMessage: (ChatMessage, ChatItem) -> Unit = { _, _ -> },
    onPinMessage: (ChatMessage, Boolean) -> Unit = { _, _ -> },
    onStarMessage: (ChatMessage, Boolean) -> Unit = { _, _ -> }
) {
    val palette = neontChatPalette()
    val isDark = isSystemInDarkTheme()
    val hazeState = remember { HazeState() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * 0.8f).dp

    var input by rememberSaveable { mutableStateOf("") }
    var replyingToId by rememberSaveable { mutableStateOf<String?>(null) }
    var pinnedId by rememberSaveable { mutableStateOf<String?>(null) }
    var starredIds by rememberSaveable(stateSaver = DetailStringSetSaver) { mutableStateOf(emptySet<String>()) }
    var actionMessageId by remember { mutableStateOf<String?>(null) }
    var editTargetId by remember { mutableStateOf<String?>(null) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }
    var forwardTargetId by remember { mutableStateOf<String?>(null) }
    var showInfoTargetId by remember { mutableStateOf<String?>(null) }

    val byId = remember(messages) { messages.filter { it.msgId.isNotBlank() }.associateBy { it.msgId } }
    val entries = remember(messages) { buildEntries(messages) }
    val newestMessage = remember(entries) { entries.firstNotNullOfOrNull { (it as? ChatEntry.Msg)?.message } }
    val replyingTo = replyingToId?.let { byId[it] }
    val pinned = pinnedId?.let { byId[it] }
    val showScrollToBottom by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    LaunchedEffect(newestMessage?.msgId) {
        val newest = newestMessage
        if (newest != null && (newest.isFromMe || listState.firstVisibleItemIndex <= 2)) {
            listState.animateScrollToItem(0)
        }
    }

    val scrollToMessage: (String) -> Unit = { msgId ->
        val index = entries.indexOfFirst { it is ChatEntry.Msg && it.message.msgId == msgId }
        if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
    }

    // Validasi input: trim, tolak kosong, max 4000 karakter
    val send: () -> Unit = {
        val text = input.trim()
        if (text.isNotEmpty() && text.length <= MAX_MESSAGE_LENGTH) {
            onSendMessage(text, replyingToId)
            input = ""
            replyingToId = null
        }
    }

    // Wallpaper Background dengan radial gradient
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(chatWallpaperBrush(isDark))
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // B. TOP BAR (LIQUID GLASS)
            // ==========================================
            ChatDetailTopBar(
                name = chatPartnerName,
                isOnline = isPartnerOnline,
                statusText = statusText,
                palette = palette,
                hazeState = hazeState,
                isDark = isDark,
                onBack = onBackClick,
                onCall = { onCallClick(false) },
                onVideo = { onCallClick(true) },
                onMore = onMoreClick,
                onTitleClick = onTopBarClick
            )

            // ==========================================
            // C. PINNED BANNER (LIQUID GLASS)
            // ==========================================
            if (pinned != null) {
                PinnedBanner(
                    message = pinned,
                    palette = palette,
                    hazeState = hazeState,
                    isDark = isDark,
                    onClick = { scrollToMessage(pinned.msgId) },
                    onUnpin = {
                        pinnedId = null
                        onPinMessage(pinned, false)
                    }
                )
            }

            // ==========================================
            // D. CHAT AREA (LazyColumn, hazeSource)
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    // H. EMPTY STATE (NEON AI GLASS CARD)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.15f),
                                    spotColor = Color.Black.copy(alpha = 0.20f)
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .hazeEffect(state = hazeState) {
                                    blurRadius = 20.dp
                                }
                                .background(
                                    if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.75f)
                                )
                                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("✨", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Neon AI",
                                color = palette.textPrimary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ada yang bisa saya bantu?",
                                color = palette.textSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            val promptSuggestions = listOf(
                                "Rekomendasi playlist musik santai",
                                "Trik produktif mengatur waktu",
                                "Tulis pesan ulang tahun kreatif"
                            )
                            promptSuggestions.forEach { prompt ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.55f)
                                        )
                                        .border(0.5.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            input = prompt
                                            send()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = prompt,
                                        color = palette.textPrimary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = hazeState)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { focusManager.clearFocus() },
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        items(
                            items = entries,
                            key = { entry ->
                                when (entry) {
                                    is ChatEntry.Day -> "day_${entry.key}"
                                    is ChatEntry.Msg -> {
                                        val m = entry.message
                                        if (m.msgId.isNotBlank()) "m_${m.msgId}" else "t_${m.timestamp}_${m.senderId}"
                                    }
                                }
                            }
                        ) { entry ->
                            when (entry) {
                                is ChatEntry.Day -> DayPill(label = entry.label, hazeState = hazeState, isDark = isDark)
                                is ChatEntry.Msg -> {
                                    val message = entry.message
                                    MessageBubbleRow(
                                        message = message,
                                        repliedTo = message.replyTo?.let { byId[it] },
                                        isGroup = isGroup,
                                        isStarred = message.msgId in starredIds,
                                        palette = palette,
                                        isDark = isDark,
                                        hazeState = hazeState,
                                        maxBubbleWidth = maxBubbleWidth,
                                        onSwipeReply = { replyingToId = message.msgId },
                                        onLongPress = { if (message.msgId.isNotBlank()) actionMessageId = message.msgId },
                                        onQuoteClick = { message.replyTo?.let(scrollToMessage) }
                                    )
                                }
                            }
                        }
                    }
                }

                // G. Tombol Melayang Scroll ke Bawah (Glass circle 40dp, shadow 4dp, KeyboardArrowDown)
                AnimatedVisibility(
                    visible = showScrollToBottom,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 72.dp),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .hazeEffect(state = hazeState) {
                                blurRadius = 15.dp
                            }
                            .background(
                                if (isDark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.85f)
                            )
                            .border(0.5.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                            .clickable { scope.launch { listState.animateScrollToItem(0) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll ke bawah",
                            tint = palette.textSecondary
                        )
                    }
                }
            }

            // ==========================================
            // F. COMPOSER (WAJIB FLOATING DI ATAS WALLPAPER)
            // ==========================================
            ChatComposer(
                value = input,
                onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) input = it },
                replyTo = replyingTo,
                palette = palette,
                hazeState = hazeState,
                isDark = isDark,
                onCancelReply = { replyingToId = null },
                onSend = send,
                onMicClick = onMicClick,
                onAttachClick = { /* Lampiran media */ }
            )
        }
    }

    // Modal Context Menu Popover Liquid Glass saat Long-Press Bubble
    val actionMessage = actionMessageId?.let { byId[it] }
    if (actionMessage != null) {
        MessageContextMenu(
            message = actionMessage,
            currentUserId = currentUserId,
            isPinned = pinnedId == actionMessage.msgId,
            isStarred = actionMessage.msgId in starredIds,
            palette = palette,
            onDismiss = { actionMessageId = null },
            onReaction = { emoji ->
                onReaction(actionMessage, emoji)
                actionMessageId = null
            },
            onReply = {
                replyingToId = actionMessage.msgId
                actionMessageId = null
            },
            onCopy = {
                actionMessageId = null
            },
            onForward = {
                forwardTargetId = actionMessage.msgId
                actionMessageId = null
            },
            onTogglePin = {
                val shouldPin = pinnedId != actionMessage.msgId
                pinnedId = if (shouldPin) actionMessage.msgId else null
                onPinMessage(actionMessage, shouldPin)
                actionMessageId = null
            },
            onToggleStar = {
                val shouldStar = actionMessage.msgId !in starredIds
                starredIds = if (shouldStar) starredIds + actionMessage.msgId else starredIds - actionMessage.msgId
                onStarMessage(actionMessage, shouldStar)
                actionMessageId = null
            },
            onEdit = {
                editTargetId = actionMessage.msgId
                actionMessageId = null
            },
            onDelete = {
                deleteTargetId = actionMessage.msgId
                actionMessageId = null
            },
            onMessageInfo = {
                showInfoTargetId = actionMessage.msgId
                actionMessageId = null
            },
            onShare = {
                actionMessageId = null
            },
            onSaveMedia = {
                actionMessageId = null
            },
            onSelectMode = {
                actionMessageId = null
            }
        )
    }

    // Dialog Edit Pesan
    val editTarget = editTargetId?.let { byId[it] }
    if (editTarget != null) {
        EditMessageDialog(
            message = editTarget,
            palette = palette,
            onDismiss = { editTargetId = null },
            onConfirm = { newText ->
                onEditMessage(editTarget, newText)
                editTargetId = null
            }
        )
    }

    // Dialog Konfirmasi Hapus Pesan
    val deleteTarget = deleteTargetId?.let { byId[it] }
    if (deleteTarget != null) {
        DeleteMessageDialog(
            canDeleteForEveryone = deleteTarget.isFromMe && !deleteTarget.isDeleted,
            palette = palette,
            onDismiss = { deleteTargetId = null },
            onDelete = { forEveryone ->
                onDeleteMessage(deleteTarget, forEveryone)
                deleteTargetId = null
            }
        )
    }

    // Dialog Teruskan Pesan ke Kontak / Grup Lain
    val forwardTarget = forwardTargetId?.let { byId[it] }
    if (forwardTarget != null) {
        ForwardDialog(
            targets = forwardTargets,
            palette = palette,
            onDismiss = { forwardTargetId = null },
            onSelect = { chat ->
                onForwardMessage(forwardTarget, chat)
                forwardTargetId = null
            }
        )
    }

    // Dialog Info Pesan
    val infoTarget = showInfoTargetId?.let { byId[it] }
    if (infoTarget != null) {
        MessageInfoDialog(
            message = infoTarget,
            palette = palette,
            onDismiss = { showInfoTargetId = null }
        )
    }
}

/**
 * Top Bar Chat Detail Liquid Glass (WhatsApp iOS 26)
 * Row padding 4dp/4dp, hazeEffect blur 15dp alpha 0.7, tanpa border tebal
 */
@Composable
private fun ChatDetailTopBar(
    name: String,
    isOnline: Boolean,
    statusText: String?,
    palette: NeontChatPalette,
    hazeState: HazeState,
    isDark: Boolean,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onVideo: () -> Unit,
    onMore: () -> Unit,
    onTitleClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hazeEffect(state = hazeState) {
                blurRadius = 15.dp
            }
            .background(
                if (isDark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.72f)
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(0.dp))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = IosBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTitleClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeontAvatar(name = name, size = 36.dp, ringColor = palette.composerBackground)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = name,
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val status = if (isOnline) "online" else statusText
                    if (!status.isNullOrBlank()) {
                        Text(
                            text = status,
                            color = if (isOnline) palette.primary else palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(onClick = onVideo) {
                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = IosBlue, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onCall) {
                Icon(Icons.Default.Call, contentDescription = "Telepon", tint = IosBlue, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Default.MoreVert, contentDescription = "Lainnya", tint = IosBlue, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Banner Pesan yang Disematkan (Glass alpha 0.6, Icon PushPin 16dp #25D366, text 12sp, close 20dp)
 */
@Composable
private fun PinnedBanner(
    message: ChatMessage,
    palette: NeontChatPalette,
    hazeState: HazeState,
    isDark: Boolean,
    onClick: () -> Unit,
    onUnpin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hazeEffect(state = hazeState) {
                blurRadius = 15.dp
            }
            .background(
                if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.60f)
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(0.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pesan disematkan",
                    color = Color(0xFF25D366),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (message.isDeleted) "Pesan ini telah dihapus" else message.text,
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Lepas sematan",
                tint = palette.textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onUnpin)
            )
        }
    }
}

/**
 * Pemisah Hari (Day Pill Glass rounded 10dp, text 12sp #8E8E93)
 */
@Composable
private fun DayPill(label: String, hazeState: HazeState, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .hazeEffect(state = hazeState) {
                    blurRadius = 10.dp
                }
                .background(
                    if (isDark) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.70f)
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

/**
 * Container Geser untuk Membalas (Swipe to Reply)
 */
@Composable
private fun SwipeToReplyContainer(
    enabled: Boolean,
    onReply: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 56.dp.toPx() }
    val maxPx = with(density) { 88.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val latestOnReply by rememberUpdatedState(onReply)
    val palette = neontChatPalette()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                var triggered = false
                var dragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        triggered = false
                        dragX = offsetX.value
                    },
                    onDragEnd = { scope.launch { offsetX.animateTo(0f) } },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragX = (dragX + dragAmount).coerceIn(0f, maxPx)
                        scope.launch { offsetX.snapTo(dragX) }
                        if (!triggered && dragX >= thresholdPx) {
                            triggered = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            latestOnReply()
                        }
                    }
                )
            }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(20.dp)
                .graphicsLayer { alpha = (offsetX.value / thresholdPx).coerceIn(0f, 1f) }
        )
        Box(modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }) {
            content()
        }
    }
}

/**
 * Bubble WhatsApp iOS 26 Liquid Glass:
 * Outgoing: #DCF8C6 (light) / #005C4B (dark), Shape RoundedCornerShape(18,18,18,4), TANPA BORDER
 * Incoming: #FFFFFF (light) / #202C33 (dark), Shape RoundedCornerShape(18,18,4,18), TANPA BORDER
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleRow(
    message: ChatMessage,
    repliedTo: ChatMessage?,
    isGroup: Boolean,
    isStarred: Boolean,
    palette: NeontChatPalette,
    isDark: Boolean,
    hazeState: HazeState,
    maxBubbleWidth: Dp,
    onSwipeReply: () -> Unit,
    onLongPress: () -> Unit,
    onQuoteClick: () -> Unit
) {
    val fromMe = message.isFromMe
    val haptic = LocalHapticFeedback.current

    val bubbleShape = if (fromMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 4.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
    }

    val bubbleBg = if (fromMe) {
        if (isDark) Color(0xFF005C4B) else Color(0xFFDCF8C6)
    } else {
        if (isDark) Color(0xFF202C33) else Color(0xFFFFFFFF)
    }

    val reactionCounts = remember(message.reactions) {
        message.reactions.values.groupingBy { it }.eachCount()
    }

    SwipeToReplyContainer(
        enabled = !message.isDeleted && message.msgId.isNotBlank(),
        onReply = onSwipeReply
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start
        ) {
            Column(horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start) {
                // Container Bubble tanpa border
                Column(
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .shadow(
                            elevation = 2.dp,
                            shape = bubbleShape,
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.12f)
                        )
                        .clip(bubbleShape)
                        .background(bubbleBg)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Pengirim di dalam grup
                    if (!fromMe && isGroup && message.senderName.isNotBlank()) {
                        Text(
                            text = message.senderName,
                            color = palette.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Kutipan Pesan yang Dibalas (Quote Reply: border kiri 3dp #25D366, glass background alpha 0.15)
                    if (message.replyTo != null && !message.isDeleted) {
                        ReplyQuoteBanner(repliedTo = repliedTo, palette = palette, onClick = onQuoteClick)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Konten Teks Pesan (15sp)
                    if (message.isDeleted) {
                        Text(
                            text = "Pesan ini telah dihapus",
                            color = Color(0xFF8E8E93),
                            fontSize = 15.sp,
                            fontFamily = InterFontFamily,
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
                        )
                    } else {
                        Text(
                            text = message.text,
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontFamily = InterFontFamily,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Baris Informasi: Berbintang, Diedit, Waktu (11sp #8E8E93), Centang
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isStarred) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Berbintang",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        if (message.isEdited && !message.isDeleted) {
                            Text(text = "Diedit", color = Color(0xFF8E8E93), fontSize = 11.sp, fontFamily = InterFontFamily)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(text = message.time, color = Color(0xFF8E8E93), fontSize = 11.sp, fontFamily = InterFontFamily)
                        if (fromMe && !message.isDeleted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = if (message.isRead) "Dibaca" else "Terkirim",
                                tint = if (message.isRead) ReadTickColor else Color(0xFF8E8E93),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Reaction Chip Liquid Glass (Glass rounded 12dp, padding 6dp, emoji 13sp + count 11sp)
                if (reactionCounts.isNotEmpty() && !message.isDeleted) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reactionCounts.forEach { (emoji, count) ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .hazeEffect(state = hazeState) {
                                        blurRadius = 10.dp
                                    }
                                    .background(
                                        if (isDark) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.70f)
                                    )
                                    .border(0.5.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, fontSize = 13.sp)
                                if (count > 1) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = count.toString(),
                                        color = Color(0xFF8E8E93),
                                        fontSize = 11.sp,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kotak Balasan Pesan (Reply Quote: border kiri 3dp #25D366, glass background alpha 0.15)
 */
@Composable
private fun ReplyQuoteBanner(
    repliedTo: ChatMessage?,
    palette: NeontChatPalette,
    onClick: () -> Unit
) {
    val author = when {
        repliedTo == null -> ""
        repliedTo.isFromMe -> "Anda"
        else -> repliedTo.senderName
    }
    val snippet = when {
        repliedTo == null -> "Pesan yang dibalas"
        repliedTo.isDeleted -> "Pesan ini telah dihapus"
        else -> repliedTo.text
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
    ) {
        // Border Kiri 3dp #25D366
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Color(0xFF25D366))
        )
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (author.isNotBlank()) {
                Text(
                    text = author,
                    color = Color(0xFF25D366),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = snippet,
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Composer Pesan (WAJIB FLOATING DI ATAS WALLPAPER):
 * - Floating di atas wallpaper, padding horizontal 8dp, bottom 8dp
 * - Container: hazeEffect (blur 20dp) alpha 0.7, rounded 22dp, border 0.5dp White alpha 0.35
 * - Input: BasicTextField, maxLines 5, placeholder "Ketik pesan" 17sp #8E8E93
 * - Send/Mic: glass circle 44dp, Brush.linearGradient(#25D366, #128C7E), border 0.5dp White alpha 0.4
 */
@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    replyTo: ChatMessage?,
    palette: NeontChatPalette,
    hazeState: HazeState,
    isDark: Boolean,
    onCancelReply: () -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit
) {
    val hasText = value.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        // Pratinjau Balasan Floating
        AnimatedVisibility(visible = replyTo != null) {
            if (replyTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .hazeEffect(state = hazeState) {
                            blurRadius = 15.dp
                        }
                        .background(
                            if (isDark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.75f)
                        )
                        .border(0.5.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                        .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ReplyQuoteBanner(repliedTo = replyTo, palette = palette, onClick = {})
                    }
                    IconButton(onClick = onCancelReply) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Batal",
                            tint = palette.textSecondary
                        )
                    }
                }
            }
        }

        // Floating Glass Composer Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.15f),
                    spotColor = Color.Black.copy(alpha = 0.20f)
                )
                .clip(RoundedCornerShape(22.dp))
                .hazeEffect(state = hazeState) {
                    blurRadius = 20.dp
                }
                .background(
                    if (isDark) Color.Black.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.72f)
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Ikon Tambah (+) Media di Sisi Kiri
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Lampirkan",
                    tint = IosBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Input Teks
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Ketik pesan",
                        color = Color(0xFF8E8E93),
                        fontSize = 17.sp,
                        fontFamily = InterFontFamily
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    maxLines = 5,
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 17.sp,
                        fontFamily = InterFontFamily
                    ),
                    cursorBrush = SolidColor(palette.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Tombol Kirim / Pesan Suara (Glass Circle 44dp, Brush linearGradient #25D366, #128C7E, border 0.5dp White alpha 0.4)
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = hasText,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(6.dp, CircleShape, ambientColor = Color(0xFF25D366).copy(alpha = 0.35f))
                            .clip(CircleShape)
                            .background(glassGreenGradientBrush())
                            .border(0.5.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                            .clickable(onClick = onSend),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !hasText,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(6.dp, CircleShape, ambientColor = Color(0xFF25D366).copy(alpha = 0.35f))
                            .clip(CircleShape)
                            .background(glassGreenGradientBrush())
                            .border(0.5.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                            .clickable(onClick = onMicClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Pesan suara",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog Hapus Pesan
 */
@Composable
private fun DeleteMessageDialog(
    canDeleteForEveryone: Boolean,
    palette: NeontChatPalette,
    onDismiss: () -> Unit,
    onDelete: (forEveryone: Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(palette.surface)
                .border(0.5.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
        ) {
            Text(
                text = "Hapus pesan?",
                color = palette.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
            )
            Text(
                text = "Pesan yang dihapus tidak dapat dipulihkan.",
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            )
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

            Text(
                text = "Hapus untuk Saya",
                color = palette.destructive,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDelete(false) }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            )

            if (canDeleteForEveryone) {
                HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                Text(
                    text = "Hapus untuk Semua Orang",
                    color = palette.destructive,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDelete(true) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }

            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            Text(
                text = "Batal",
                color = IosBlue,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            )
        }
    }
}

/**
 * Dialog Edit Teks Pesan
 */
@Composable
private fun EditMessageDialog(
    message: ChatMessage,
    palette: NeontChatPalette,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(message.msgId) { mutableStateOf(message.text) }
    val canConfirm = text.isNotBlank() && text.trim() != message.text

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("Edit pesan", color = palette.textPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) text = it },
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = canConfirm
            ) {
                Text("Simpan", color = if (canConfirm) IosBlue else palette.textSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = palette.textSecondary)
            }
        }
    )
}

/**
 * Dialog Meneruskan Pesan
 */
@Composable
private fun ForwardDialog(
    targets: List<ChatItem>,
    palette: NeontChatPalette,
    onDismiss: () -> Unit,
    onSelect: (ChatItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("Teruskan ke", color = palette.textPrimary) },
        text = {
            if (targets.isEmpty()) {
                Text("Belum ada chat tujuan yang tersedia.", color = palette.textSecondary)
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    targets.forEach { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(chat) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeontAvatar(name = chat.chatName, size = 36.dp, ringColor = palette.surface)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = chat.chatName,
                                color = palette.textPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = IosBlue)
            }
        }
    )
}

/**
 * Dialog Informasi Pesan
 */
@Composable
private fun MessageInfoDialog(
    message: ChatMessage,
    palette: NeontChatPalette,
    onDismiss: () -> Unit
) {
    val dateStr = remember(message.timestamp) {
        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")).format(Date(message.timestamp))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("Info Pesan", color = palette.textPrimary) },
        text = {
            Column {
                Text("Status:", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = if (message.isRead) "Dibaca" else "Terkirim",
                    color = if (message.isRead) ReadTickColor else palette.textPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Waktu:", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                Text(text = dateStr, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", color = IosBlue) }
        }
    )
}

private fun dayKey(timestamp: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
}

private fun dayLabel(timestamp: Long): String {
    val now = System.currentTimeMillis()
    return when (dayKey(timestamp)) {
        dayKey(now) -> "Hari ini"
        dayKey(now - DAY_MS) -> "Kemarin"
        else -> SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(timestamp))
    }
}

private fun buildEntries(messages: List<ChatMessage>): List<ChatEntry> {
    val result = ArrayList<ChatEntry>(messages.size + 8)
    var lastDay = Int.MIN_VALUE
    messages.sortedBy { it.timestamp }.forEach { message ->
        if (message.timestamp > 0L) {
            val day = dayKey(message.timestamp)
            if (day != lastDay) {
                result.add(ChatEntry.Day(day, dayLabel(message.timestamp)))
                lastDay = day
            }
        }
        result.add(ChatEntry.Msg(message))
    }
    result.reverse()
    return result
}

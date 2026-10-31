package app.neon.com.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import app.neon.com.data.model.ChatItem
import app.neon.com.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val MAX_MESSAGE_LENGTH = 4000
private const val DAY_MS = 24L * 60L * 60L * 1000L
private val QuickReactions = listOf("❤️", "👍", "🔥", "😂", "😢", "🙏")
private val ReadTickColor = Color(0xFF34B7F1)
private val DetailStringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() }
)

private sealed interface ChatEntry {
    data class Day(val key: Int, val label: String) : ChatEntry
    data class Msg(val message: ChatMessage) : ChatEntry
}
// sisa kode ada di Langkah 4B

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
    SecureWindowEffect()
    val palette = neontChatPalette()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
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

    val send: () -> Unit = {
        val text = input.trim()
        if (text.isNotEmpty()) { onSendMessage(text, replyingToId); input = ""; replyingToId = null }
    }

    Column(modifier = modifier.fillMaxSize().background(palette.detailBackground).imePadding()) {
        ChatDetailTopBar(
            name = chatPartnerName, isOnline = isPartnerOnline, statusText = statusText,
            palette = palette, onBack = onBackClick, onCall = { onCallClick(false) },
            onVideo = { onCallClick(true) }, onMore = onMoreClick, onTitleClick = onTopBarClick
        )
        if (pinned != null) {
            PinnedBanner(
                message = pinned, palette = palette,
                onClick = { scrollToMessage(pinned.msgId) },
                onUnpin = { pinnedId = null; onPinMessage(pinned, false) }
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("✨", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Neon AI", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ada yang bisa saya bantu hari ini?", color = palette.textSecondary, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(32.dp))
                    val prompts = listOf("Ide konten kreatif", "Cara mengatur waktu", "Buatkan puisi singkat")
                    prompts.forEach { prompt ->
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp))
                                .background(palette.surface).clickable { input = prompt; send() }
                                .padding(16.dp)
                        ) {
                            Text(prompt, color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                LazyColumn(
                state = listState, reverseLayout = true,
                modifier = Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { focusManager.clearFocus() },
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = entries,
                    key = { entry ->
                        when (entry) {
                            is ChatEntry.Day -> "d_${entry.key}"
                            is ChatEntry.Msg -> {
                                val m = entry.message
                                if (m.msgId.isNotBlank()) "m_${m.msgId}" else "t_${m.timestamp}_${m.senderId}"
                            }
                        }
                    }
                ) { entry ->
                    when (entry) {
                        is ChatEntry.Day -> DayPill(label = entry.label, palette = palette)
                        is ChatEntry.Msg -> {
                            val message = entry.message
                            MessageRow(
                                message = message,
                                repliedTo = message.replyTo?.let { byId[it] },
                                isGroup = isGroup,
                                isStarred = message.msgId in starredIds,
                                palette = palette,
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
            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollToBottom,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier.size(40.dp).shadow(4.dp, CircleShape)
                        .background(palette.surface, CircleShape).clip(CircleShape)
                        .clickable { scope.launch { listState.animateScrollToItem(0) } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Ke pesan terbaru", tint = palette.textSecondary)
                }
            }
        }
        ChatComposer(
            value = input,
            onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) input = it },
            replyTo = replyingTo, palette = palette,
            onCancelReply = { replyingToId = null },
            onSend = send, onMicClick = onMicClick
        )
    }

    val actionMessage = actionMessageId?.let { byId[it] }
    if (actionMessage != null) {
        MessageActionsDialog(
            message = actionMessage, currentUserId = currentUserId,
            isPinned = pinnedId == actionMessage.msgId, isStarred = actionMessage.msgId in starredIds,
            palette = palette, onDismiss = { actionMessageId = null },
            onReaction = { emoji -> onReaction(actionMessage, emoji); actionMessageId = null },
            onReply = { replyingToId = actionMessage.msgId; actionMessageId = null },
            onCopy = { copyToClipboard(context, actionMessage.text); actionMessageId = null },
            onForward = { forwardTargetId = actionMessage.msgId; actionMessageId = null },
            onTogglePin = {
                val pin = pinnedId != actionMessage.msgId
                pinnedId = if (pin) actionMessage.msgId else null
                onPinMessage(actionMessage, pin); actionMessageId = null
            },
            onToggleStar = {
                val star = actionMessage.msgId !in starredIds
                starredIds = if (star) starredIds + actionMessage.msgId else starredIds - actionMessage.msgId
                onStarMessage(actionMessage, star); actionMessageId = null
            },
            onEdit = { editTargetId = actionMessage.msgId; actionMessageId = null },
            onDelete = { deleteTargetId = actionMessage.msgId; actionMessageId = null }
        )
    }

    val editTarget = editTargetId?.let { byId[it] }
    if (editTarget != null) {
        EditMessageDialog(message = editTarget, palette = palette, onDismiss = { editTargetId = null }, onConfirm = { newText -> onEditMessage(editTarget, newText); editTargetId = null })
    }
    val deleteTarget = deleteTargetId?.let { byId[it] }
    if (deleteTarget != null) {
        DeleteMessageDialog(
            canDeleteForEveryone = deleteTarget.isFromMe && !deleteTarget.isDeleted, palette = palette,
            onDismiss = { deleteTargetId = null },
            onDelete = { forEveryone -> onDeleteMessage(deleteTarget, forEveryone); deleteTargetId = null }
        )
    }
    val forwardTarget = forwardTargetId?.let { byId[it] }
    if (forwardTarget != null) {
        ForwardDialog(targets = forwardTargets, palette = palette, onDismiss = { forwardTargetId = null }, onSelect = { chat -> onForwardMessage(forwardTarget, chat); forwardTargetId = null })
    }
}
// sisa kode ada di Langkah 4C

@Composable
private fun ChatDetailTopBar(
    name: String, isOnline: Boolean, statusText: String?, palette: NeontChatPalette,
    onBack: () -> Unit, onCall: () -> Unit, onVideo: () -> Unit, onMore: () -> Unit, onTitleClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(palette.composerBackground).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = palette.textPrimary) }
            Row(modifier = Modifier.weight(1f).clickable(onClick = onTitleClick), verticalAlignment = Alignment.CenterVertically) {
                NeontAvatar(name = name, size = 36.dp, ringColor = palette.composerBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(name, color = palette.textPrimary, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val status = if (isOnline) "online" else statusText
                    if (!status.isNullOrBlank()) {
                        Text(status, color = if (isOnline) palette.primary else palette.timestamp, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            IconButton(onClick = onVideo) { Icon(Icons.Default.Videocam, "Panggilan video", tint = palette.textPrimary) }
            IconButton(onClick = onCall) { Icon(Icons.Default.Call, "Panggilan suara", tint = palette.textPrimary) }
            IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, "Lainnya", tint = palette.textPrimary) }
        }
        HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
    }
}

@Composable
private fun PinnedBanner(message: ChatMessage, palette: NeontChatPalette, onClick: () -> Unit, onUnpin: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(palette.composerBackground).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PushPin, null, tint = palette.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Pesan disematkan", color = palette.primary, style = MaterialTheme.typography.labelMedium)
                Text(if (message.isDeleted) "Pesan ini telah dihapus" else message.text, color = palette.timestamp, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.Close, "Lepas sematan", tint = palette.textSecondary, modifier = Modifier.size(20.dp).clickable(onClick = onUnpin))
        }
        HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
    }
}

@Composable
private fun DayPill(label: String, palette: NeontChatPalette) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, color = palette.timestamp, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(palette.surface.copy(alpha = 0.92f)).padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

@Composable
private fun SwipeToReplyBox(enabled: Boolean, onReply: () -> Unit, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 64.dp.toPx() }
    val maxPx = with(density) { 96.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val latestOnReply by rememberUpdatedState(onReply)
    val palette = neontChatPalette()
    Box(
        modifier = Modifier.fillMaxWidth().pointerInput(enabled) {
            if (!enabled) return@pointerInput
            var triggered = false
            var dragX = 0f
            detectHorizontalDragGestures(
                onDragStart = { triggered = false; dragX = offsetX.value },
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
        Icon(Icons.AutoMirrored.Filled.Reply, null, tint = palette.textSecondary, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp).size(22.dp).graphicsLayer { alpha = (offsetX.value / thresholdPx).coerceIn(0f, 1f) })
        Box(modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }) { content() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: ChatMessage, repliedTo: ChatMessage?, isGroup: Boolean, isStarred: Boolean,
    palette: NeontChatPalette, maxBubbleWidth: Dp, onSwipeReply: () -> Unit, onLongPress: () -> Unit, onQuoteClick: () -> Unit
) {
    val fromMe = message.isFromMe
    val haptic = LocalHapticFeedback.current
    val shape = if (fromMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    val reactionCounts = remember(message.reactions) { message.reactions.values.groupingBy { it }.eachCount() }
    SwipeToReplyBox(enabled = !message.isDeleted && message.msgId.isNotBlank(), onReply = onSwipeReply) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start) {
            Column(horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start) {
                Column(
                    modifier = Modifier.widthIn(max = maxBubbleWidth).clip(shape)
                        .background(if (fromMe) palette.bubbleOutgoing else palette.bubbleIncoming)
                        .combinedClickable(onClick = {}, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() })
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (!fromMe && isGroup && message.senderName.isNotBlank()) {
                        Text(message.senderName, color = palette.primary, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    if (message.replyTo != null && !message.isDeleted) {
                        ReplyQuote(repliedTo = repliedTo, palette = palette, onClick = onQuoteClick)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    if (message.isDeleted) {
                        Text("Pesan ini telah dihapus", color = palette.timestamp, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic))
                    } else {
                        Text(message.text, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                        if (isStarred) { Icon(Icons.Default.Star, "Berbintang", tint = palette.timestamp, modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(4.dp)) }
                        if (message.isEdited && !message.isDeleted) { Text("Diedit", color = palette.timestamp, style = MaterialTheme.typography.labelSmall); Spacer(modifier = Modifier.width(4.dp)) }
                        Text(message.time, color = palette.timestamp, style = MaterialTheme.typography.labelSmall)
                        if (fromMe && !message.isDeleted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done, if (message.isRead) "Dibaca" else "Terkirim", tint = if (message.isRead) ReadTickColor else palette.timestamp, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (reactionCounts.isNotEmpty() && !message.isDeleted) {
                    Row(modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        reactionCounts.forEach { (emoji, count) ->
                            Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(palette.surface).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 13.sp)
                                if (count > 1) { Spacer(modifier = Modifier.width(2.dp)); Text(count.toString(), color = palette.timestamp, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyQuote(repliedTo: ChatMessage?, palette: NeontChatPalette, onClick: () -> Unit) {
    val author = when { repliedTo == null -> ""; repliedTo.isFromMe -> "Anda"; else -> repliedTo.senderName }
    val snippet = when { repliedTo == null -> "Pesan yang dibalas"; repliedTo.isDeleted -> "Pesan ini telah dihapus"; else -> repliedTo.text }
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(8.dp)).background(palette.textPrimary.copy(alpha = 0.08f)).clickable(onClick = onClick)) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(palette.primary))
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (author.isNotBlank()) Text(author, color = palette.primary, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(snippet, color = palette.timestamp, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChatComposer(value: String, onValueChange: (String) -> Unit, replyTo: ChatMessage?, palette: NeontChatPalette, onCancelReply: () -> Unit, onSend: () -> Unit, onMicClick: () -> Unit) {
    val inputColor = if (isSystemInDarkTheme()) Color(0xFF2A3942) else Color.White
    val hasText = value.isNotBlank()
    Column(modifier = Modifier.fillMaxWidth().background(palette.composerBackground).navigationBarsPadding()) {
        HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
        AnimatedVisibility(visible = replyTo != null) {
            if (replyTo != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) { ReplyQuote(repliedTo = replyTo, palette = palette, onClick = {}) }
                    IconButton(onClick = onCancelReply) { Icon(Icons.Default.Close, "Batalkan balasan", tint = palette.textSecondary) }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
            Box(modifier = Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(20.dp)).background(inputColor).padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text("Ketik pesan", color = palette.textSecondary, style = MaterialTheme.typography.bodyLarge)
                BasicTextField(value = value, onValueChange = onValueChange, maxLines = 5, textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary), cursorBrush = SolidColor(palette.primary), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(visible = hasText, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(palette.primary).clickable(onClick = onSend), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Kirim", tint = Color.White)
                    }
                }
                androidx.compose.animation.AnimatedVisibility(visible = !hasText, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(palette.primary).clickable(onClick = onMicClick), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Mic, "Pesan suara", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecureDialogWindow() {
    val view = LocalView.current
    DisposableEffect(view) {
        (view.parent as? DialogWindowProvider)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { }
    }
}

@Composable
private fun ActionItem(text: String, color: Color, onClick: () -> Unit) {
    Text(text, color = color, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp))
}

@Composable
private fun MessageActionsDialog(message: ChatMessage, currentUserId: String, isPinned: Boolean, isStarred: Boolean, palette: NeontChatPalette, onDismiss: () -> Unit, onReaction: (String) -> Unit, onReply: () -> Unit, onCopy: () -> Unit, onForward: () -> Unit, onTogglePin: () -> Unit, onToggleStar: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isDeleted = message.isDeleted
    Dialog(onDismissRequest = onDismiss) {
        SecureDialogWindow()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!isDeleted) {
                Row(modifier = Modifier.clip(RoundedCornerShape(28.dp)).background(palette.surface).padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    QuickReactions.forEach { emoji ->
                        val isMine = message.reactions[currentUserId] == emoji
                        Box(modifier = Modifier.clip(CircleShape).background(if (isMine) palette.primary.copy(alpha = 0.25f) else Color.Transparent).clickable { onReaction(emoji) }.padding(6.dp), contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 26.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Column(modifier = Modifier.width(260.dp).clip(RoundedCornerShape(14.dp)).background(palette.surface)) {
                if (!isDeleted) {
                    ActionItem("Balas", palette.textPrimary, onReply)
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                    ActionItem("Salin", palette.textPrimary, onCopy)
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                    ActionItem("Teruskan", palette.textPrimary, onForward)
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                    ActionItem(if (isPinned) "Lepas sematan" else "Sematkan", palette.textPrimary, onTogglePin)
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                    ActionItem(if (isStarred) "Hapus bintang" else "Bintangi", palette.textPrimary, onToggleStar)
                    if (message.isFromMe) { HorizontalDivider(color = palette.divider, thickness = 0.5.dp); ActionItem("Edit", palette.textPrimary, onEdit) }
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                }
                ActionItem("Hapus", palette.destructive, onDelete)
            }
        }
    }
}

@Composable
private fun DeleteMessageDialog(canDeleteForEveryone: Boolean, palette: NeontChatPalette, onDismiss: () -> Unit, onDelete: (forEveryone: Boolean) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        SecureDialogWindow()
        Column(modifier = Modifier.width(280.dp).clip(RoundedCornerShape(14.dp)).background(palette.surface)) {
            Text("Hapus pesan?", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
            ActionItem("Hapus untuk Saya", palette.destructive) { onDelete(false) }
            if (canDeleteForEveryone) { HorizontalDivider(color = palette.divider, thickness = 0.5.dp); ActionItem("Hapus untuk Semua Orang", palette.destructive) { onDelete(true) } }
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            ActionItem("Batal", palette.textPrimary, onDismiss)
        }
    }
}

@Composable
private fun EditMessageDialog(message: ChatMessage, palette: NeontChatPalette, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(message.msgId) { mutableStateOf(message.text) }
    val canConfirm = text.isNotBlank() && text.trim() != message.text
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = palette.surface,
        title = { Text("Edit pesan") },
        text = { OutlinedTextField(value = text, onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) text = it }, maxLines = 5, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }, enabled = canConfirm) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun ForwardDialog(targets: List<ChatItem>, palette: NeontChatPalette, onDismiss: () -> Unit, onSelect: (ChatItem) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = palette.surface,
        title = { Text("Teruskan ke") },
        text = {
            if (targets.isEmpty()) Text("Belum ada chat tujuan.")
            else Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                targets.forEach { chat ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(chat) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        NeontAvatar(name = chat.chatName, size = 36.dp, ringColor = palette.surface)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(chat.chatName, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("pesan", text))
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
            if (day != lastDay) { result.add(ChatEntry.Day(day, dayLabel(message.timestamp))); lastDay = day }
        }
        result.add(ChatEntry.Msg(message))
    }
    result.reverse()
    return result
}

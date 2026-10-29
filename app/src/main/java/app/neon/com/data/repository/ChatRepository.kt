package app.neon.com.data.repository

import app.neon.com.data.model.ChatItem
import app.neon.com.data.model.ChatMessage
import app.neon.com.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatRepository {
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    private val localStore = mutableMapOf<String, MutableList<ChatMessage>>()

    init {
        val defaultChatId = "chat_aura"
        localStore[defaultChatId] = mutableListOf(
            ChatMessage(
                msgId = "m_1", chatId = defaultChatId, senderId = "aura_1", senderName = "Aura Cantika",
                text = "Hai! Lagi dengerin lagu apa di Neont? 🎧", time = "10:35",
                timestamp = System.currentTimeMillis() - 10 * 60 * 1000, isFromMe = false, isRead = true,
                reactions = mapOf("my_uid" to "❤️")
            ),
            ChatMessage(
                msgId = "m_2", chatId = defaultChatId, senderId = "my_uid", senderName = "Saya",
                text = "Lagi dengerin pop hits terbaru, audionya jernih banget!", time = "10:38",
                timestamp = System.currentTimeMillis() - 7 * 60 * 1000, isFromMe = true, isRead = true
            ),
            ChatMessage(
                msgId = "m_3", chatId = defaultChatId, senderId = "aura_1", senderName = "Aura Cantika",
                text = "Coba dengerin playlist ini, barusan ku-share di grup musik", time = "10:40",
                timestamp = System.currentTimeMillis() - 5 * 60 * 1000, isFromMe = false, isRead = true
            ),
            ChatMessage(
                msgId = "m_4", chatId = defaultChatId, senderId = "aura_1", senderName = "Aura Cantika",
                text = "Lagu yang barusan kamu putar di Neont enak banget! 🎶", time = "10:42",
                timestamp = System.currentTimeMillis() - 3 * 60 * 1000, isFromMe = false, isRead = true,
                reactions = mapOf("my_uid" to "🔥")
            )
        )
    }

    fun observeMessages(chatId: String, currentUserId: String): Flow<List<ChatMessage>> {
        return callbackFlow {
            val cached = localStore[chatId] ?: emptyList()
            trySend(cached)

            if (firestore != null) {
                val collectionRef = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)

                val listenerRegistration = collectionRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localStore[chatId] ?: emptyList())
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            val msgId = doc.id
                            val text = doc.getString("text") ?: ""
                            val senderId = doc.getString("senderId") ?: ""
                            val senderName = doc.getString("senderName") ?: ""
                            val time = doc.getString("time") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val isRead = doc.getBoolean("isRead") ?: false
                            val isEdited = doc.getBoolean("isEdited") ?: false
                            val isDeleted = doc.getBoolean("isDeleted") ?: false
                            val replyTo = doc.getString("replyTo")
                            @Suppress("UNCHECKED_CAST")
                            val reactions = (doc.get("reactions") as? Map<String, String>) ?: emptyMap()

                            ChatMessage(
                                msgId = msgId, chatId = chatId, senderId = senderId, senderName = senderName,
                                text = text, time = time, timestamp = timestamp,
                                isFromMe = senderId == currentUserId, isRead = isRead, isEdited = isEdited,
                                isDeleted = isDeleted, replyTo = replyTo, reactions = reactions
                            )
                        }
                        localStore[chatId] = messages.toMutableList()
                        trySend(messages)
                    } else if (snapshot != null && snapshot.isEmpty) {
                        val local = localStore[chatId] ?: emptyList()
                        trySend(local)
                    }
                }

                awaitClose { listenerRegistration.remove() }
            } else {
                awaitClose { }
            }
        }
            .debounce(100L)
            .flowOn(Dispatchers.IO)
    }

    fun observeUserChats(userId: String): Flow<List<ChatItem>> {
        return flow {
            val chats = listOf(
                ChatItem(
                    id = "chat_aura", isGroup = false, chatName = "Aura Cantika",
                    recipientId = "aura_1", recipientUsername = "Aura Cantika",
                    recipientAvatarColor = "#7C3AED", recipientAvatarUrl = ""
                ),
                ChatItem(
                    id = "chat_beni", isGroup = false, chatName = "Beni",
                    recipientId = "beni_1", recipientUsername = "Beni",
                    recipientAvatarColor = "#22C55E", recipientAvatarUrl = ""
                )
            )
            emit(chats)
        }.flowOn(Dispatchers.IO)
    }

    suspend fun sendMessage(
        chatId: String, text: String, currentUserId: String, currentUserName: String, replyTo: String? = null
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        val msgId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val newMessage = ChatMessage(
            msgId = msgId, chatId = chatId, senderId = currentUserId, senderName = currentUserName,
            text = text.trim(), time = formattedTime, timestamp = System.currentTimeMillis(),
            isFromMe = true, isRead = false, replyTo = replyTo
        )

        val currentList = localStore.getOrPut(chatId) { mutableListOf() }
        currentList.add(newMessage)

        if (firestore != null) {
            runCatching {
                val messageMap = hashMapOf<String, Any>(
                    "msgId" to msgId, "chatId" to chatId, "senderId" to currentUserId,
                    "senderName" to currentUserName, "text" to text.trim(), "time" to formattedTime,
                    "timestamp" to System.currentTimeMillis(), "isRead" to false, "isEdited" to false,
                    "isDeleted" to false, "reactions" to emptyMap<String, String>()
                )
                if (replyTo != null) messageMap["replyTo"] = replyTo
                firestore.collection("chats").document(chatId).collection("messages").document(msgId).set(messageMap).await()
                firestore.collection("chats").document(chatId).set(
                    mapOf("lastMessage" to text.trim(), "lastUpdated" to System.currentTimeMillis(), "lastSenderId" to currentUserId),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            }
        }
        Result.success(newMessage)
    }

    suspend fun sendReaction(chatId: String, msgId: String, userId: String, emoji: String): Result<Unit> = withContext(Dispatchers.IO) {
        val list = localStore[chatId]
        if (list != null) {
            val index = list.indexOfFirst { it.msgId == msgId }
            if (index != -1) {
                val target = list[index]
                val currentReactions = target.reactions.toMutableMap()
                if (currentReactions[userId] == emoji) currentReactions.remove(userId) else currentReactions[userId] = emoji
                list[index] = target.copy(reactions = currentReactions)
            }
        }
        if (firestore != null) {
            runCatching {
                firestore.collection("chats").document(chatId).collection("messages").document(msgId).update("reactions.$userId", emoji).await()
            }
        }
        Result.success(Unit)
    }

    suspend fun deleteMessage(chatId: String, msgId: String, deleteForEveryone: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        val list = localStore[chatId]
        if (list != null) {
            val index = list.indexOfFirst { it.msgId == msgId }
            if (index != -1) {
                if (deleteForEveryone) list[index] = list[index].copy(isDeleted = true, text = "🚫 Pesan ini telah dihapus")
                else list.removeAt(index)
            }
        }
        if (firestore != null) {
            runCatching {
                val docRef = firestore.collection("chats").document(chatId).collection("messages").document(msgId)
                if (deleteForEveryone) docRef.update(mapOf("isDeleted" to true, "text" to "🚫 Pesan ini telah dihapus")).await()
                else docRef.delete().await()
            }
        }
        Result.success(Unit)
    }

    suspend fun editMessage(chatId: String, msgId: String, newText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val list = localStore[chatId]
        if (list != null) {
            val index = list.indexOfFirst { it.msgId == msgId }
            if (index != -1) list[index] = list[index].copy(text = newText.trim(), isEdited = true)
        }
        if (firestore != null) {
            runCatching {
                firestore.collection("chats").document(chatId).collection("messages").document(msgId)
                    .update(mapOf("text" to newText.trim(), "isEdited" to true, "editedAt" to System.currentTimeMillis())).await()
            }
        }
        Result.success(Unit)
    }

    fun searchContactByUsername(query: String): Flow<Result<User?>> = flow {
        try {
            val ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("usernames").child(query.lowercase().trim())
            ref.get().await().let { snapshot ->
                val uid = snapshot.getValue(String::class.java)
                if (uid != null) {
                    val userRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(uid)
                    userRef.get().await().let { userSnap ->
                        emit(Result.success(userSnap.getValue(User::class.java)))
                    }
                } else emit(Result.success(null))
            }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }
}

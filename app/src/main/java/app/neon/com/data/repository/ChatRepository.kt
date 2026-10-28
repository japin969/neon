package app.neon.com.data.repository

import app.neon.com.data.model.ChatMessage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * ChatRepository - Anti-Lag & Anti-Spam Firebase Firestore Repository
 * Dilengkapi:
 * 1. Flow .debounce(100L) untuk listener snapshot Firebase (anti-lag saat banjir pesan/spam).
 * 2. Operasi CRUD Firestore: sendMessage, sendReaction, deleteMessage, editMessage.
 * 3. Fallback in-memory cache jika Firestore offline / belum terinisialisasi.
 */
class ChatRepository {

    private val firestore: FirebaseFirestore? = runCatching {
        FirebaseFirestore.getInstance()
    }.getOrNull()

    // Local in-memory store for instant responsiveness & offline fallback
    private val localStore = mutableMapOf<String, MutableList<ChatMessage>>()

    init {
        // Seed default chat data for demo
        val defaultChatId = "chat_aura"
        localStore[defaultChatId] = mutableListOf(
            ChatMessage(
                msgId = "m_1",
                chatId = defaultChatId,
                senderId = "aura_1",
                senderName = "Aura Cantika",
                text = "Hai! Lagi dengerin lagu apa di Neont? 🎧",
                time = "10:35",
                timestamp = System.currentTimeMillis() - 10 * 60 * 1000,
                isFromMe = false,
                isRead = true,
                reactions = mapOf("my_uid" to "❤️")
            ),
            ChatMessage(
                msgId = "m_2",
                chatId = defaultChatId,
                senderId = "my_uid",
                senderName = "Saya",
                text = "Lagi dengerin pop hits terbaru, audionya jernih banget!",
                time = "10:38",
                timestamp = System.currentTimeMillis() - 7 * 60 * 1000,
                isFromMe = true,
                isRead = true
            ),
            ChatMessage(
                msgId = "m_3",
                chatId = defaultChatId,
                senderId = "aura_1",
                senderName = "Aura Cantika",
                text = "Coba dengerin playlist ini, barusan ku-share di grup musik",
                time = "10:40",
                timestamp = System.currentTimeMillis() - 5 * 60 * 1000,
                isFromMe = false,
                isRead = true
            ),
            ChatMessage(
                msgId = "m_4",
                chatId = defaultChatId,
                senderId = "aura_1",
                senderName = "Aura Cantika",
                text = "Lagu yang barusan kamu putar di Neont enak banget! 🎶",
                time = "10:42",
                timestamp = System.currentTimeMillis() - 3 * 60 * 1000,
                isFromMe = false,
                isRead = true,
                reactions = mapOf("my_uid" to "🔥")
            )
        )
    }

    /**
     * Mengamati pesan secara realtime dari Firestore dengan .debounce(100L) anti-lag.
     */
    fun observeMessages(chatId: String, currentUserId: String): Flow<List<ChatMessage>> {
        return callbackFlow {
            // Emit data lokal terlebih dahulu untuk kecepatan instan
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
                                msgId = msgId,
                                chatId = chatId,
                                senderId = senderId,
                                senderName = senderName,
                                text = text,
                                time = time,
                                timestamp = timestamp,
                                isFromMe = (senderId == currentUserId),
                                isRead = isRead,
                                isEdited = isEdited,
                                isDeleted = isDeleted,
                                replyTo = replyTo,
                                reactions = reactions
                            )
                        }
                        localStore[chatId] = messages.toMutableList()
                        trySend(messages)
                    } else if (snapshot != null && snapshot.isEmpty) {
                        val local = localStore[chatId] ?: emptyList()
                        trySend(local)
                    }
                }

                awaitClose {
                    listenerRegistration.remove()
                }
            } else {
                // Standalone / offline mode
                awaitClose { }
            }
        }
        .debounce(100L) // 🚀 ANTI-LAG: Mencegah jank saat ratusan pesan masuk per detik
        .flowOn(Dispatchers.IO)
    }

    /**
     * Mengirim pesan baru ke Firebase Firestore dan update metadata percakapan
     */
    suspend fun sendMessage(
        chatId: String,
        text: String,
        currentUserId: String,
        currentUserName: String,
        replyTo: String? = null
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        val msgId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val newMessage = ChatMessage(
            msgId = msgId,
            chatId = chatId,
            senderId = currentUserId,
            senderName = currentUserName,
            text = text.trim(),
            time = formattedTime,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            isRead = false,
            replyTo = replyTo
        )

        // Simpan ke local cache
        val currentList = localStore.getOrPut(chatId) { mutableListOf() }
        currentList.add(newMessage)

        if (firestore != null) {
            runCatching {
                val messageMap = hashMapOf<String, Any>(
                    "msgId" to msgId,
                    "chatId" to chatId,
                    "senderId" to currentUserId,
                    "senderName" to currentUserName,
                    "text" to text.trim(),
                    "time" to formattedTime,
                    "timestamp" to System.currentTimeMillis(),
                    "isRead" to false,
                    "isEdited" to false,
                    "isDeleted" to false,
                    "reactions" to emptyMap<String, String>()
                )
                if (replyTo != null) {
                    messageMap["replyTo"] = replyTo
                }

                // Simpan ke Firestore
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msgId)
                    .set(messageMap)
                    .await()

                // Update metadata percakapan (lastMessage, lastUpdated)
                firestore.collection("chats")
                    .document(chatId)
                    .set(
                        mapOf(
                            "lastMessage" to text.trim(),
                            "lastUpdated" to System.currentTimeMillis(),
                            "lastSenderId" to currentUserId
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()
            }
        }

        Result.success(newMessage)
    }

    /**
     * Mengirim reaksi emoji (Long-press bubble) dan menyimpan ke Firebase
     */
    suspend fun sendReaction(
        chatId: String,
        msgId: String,
        userId: String,
        emoji: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // Update local cache
        val list = localStore[chatId]
        if (list != null) {
            val index = list.indexOfFirst { it.msgId == msgId }
            if (index != -1) {
                val target = list[index]
                val currentReactions = target.reactions.toMutableMap()
                if (currentReactions[userId] == emoji) {
                    currentReactions.remove(userId) // Toggle off jika emoji sama
                } else {
                    currentReactions[userId] = emoji
                }
                list[index] = target.copy(reactions = currentReactions)
            }
        }

        // Simpan ke Firestore
        if (firestore != null) {
            runCatching {
                val docRef = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msgId)

                docRef.update("reactions.$userId", emoji).await()
            }
        }

        Result.success(Unit)
    }

    /**
     * Menghapus pesan di Firestore (Soft delete atau hard delete)
     */
    suspend fun deleteMessage(
        chatId: String,
        msgId: String,
        deleteForEveryone: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // Update local cache
        val list = localStore[chatId]
        if (list != null) {
            val index = list.indexOfFirst { it.msgId == msgId }
            if (index != -1) {
                if (deleteForEveryone) {
                    list[index] = list[index].copy(
                        isDeleted = true,
                        text = "🚫 Pesan ini telah dihapus"
                    )
                } else {
                    list.removeAt(index)
                }
            }
        }

        // Update Firestore
        if (firestore != null) {
            runCatching {
                val docRef = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msgId)

                if (deleteForEveryone) {
                    docRef.update(
                        mapOf(
                            "isDeleted" to true,
                            "text" to "🚫 Pesan ini telah dihapus"
                        )
                    ).await()
                } else {
                    docRef.delete().await()
                }
            }
        }

        Result.success(Unit)
    }

    /**
     * Mengedit pesan di Firestore
     */
    suspend fun editMessage(
        chatId: String,
        msgId: String,
        newText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // Update local cache
        val list = localStore[chatId]
        if (list != null) {
            val index = list.indexOfFirst { it.msgId == msgId }
            if (index != -1) {
                list[index] = list[index].copy(
                    text = newText.trim(),
                    isEdited = true
                )
            }
        }

        // Update Firestore
        if (firestore != null) {
            runCatching {
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msgId)
                    .update(
                        mapOf(
                            "text" to newText.trim(),
                            "isEdited" to true,
                            "editedAt" to System.currentTimeMillis()
                        )
                    ).await()
            }
        }

        Result.success(Unit)
    }
}

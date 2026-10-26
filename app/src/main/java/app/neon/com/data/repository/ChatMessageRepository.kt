package app.neon.com.data.repository

import app.neon.com.data.crypto.MessageCrypto
import app.neon.com.data.model.ChatMessage
import app.neon.com.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMessageRepository @Inject constructor() {

    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    /**
     * Observes real-time messages in a specific chat room.
     * Automatically applies AES-256 decryption, filters out messages deleted for the user,
     * and handles messages deleted for everyone.
     */
    fun observeMessages(chatId: String, currentUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val messagesRef = firebaseDatabase.getReference("messages").child(chatId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = mutableListOf<ChatMessage>()

                for (child in snapshot.children) {
                    val messageId = child.key ?: continue
                    val senderId = child.child("senderId").getValue(String::class.java) ?: ""
                    val senderUsername = child.child("senderUsername").getValue(String::class.java) ?: "User"
                    val senderAvatarColor = child.child("senderAvatarColor").getValue(String::class.java) ?: "#00FF66"
                    val encryptedText = child.child("encryptedText").getValue(String::class.java) ?: ""
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val isEdited = child.child("isEdited").getValue(Boolean::class.java) ?: false
                    val deletedForEveryone = child.child("deletedForEveryone").getValue(Boolean::class.java) ?: false
                    val isPinned = child.child("isPinned").getValue(Boolean::class.java) ?: false
                    val forwardedFrom = child.child("forwardedFrom").getValue(String::class.java) ?: ""

                    // Check if deleted for current user
                    val isDeletedForMe = child.child("deletedForUsers")
                        .child(currentUserId)
                        .getValue(Boolean::class.java) == true

                    if (isDeletedForMe) continue

                    // Parse reactions map
                    val reactionsMap = mutableMapOf<String, String>()
                    val reactionsSnapshot = child.child("reactions")
                    for (rChild in reactionsSnapshot.children) {
                        val uid = rChild.key ?: continue
                        val emoji = rChild.getValue(String::class.java) ?: continue
                        reactionsMap[uid] = emoji
                    }

                    // Parse reply reference
                    val replyToMessageId = child.child("replyToMessageId").getValue(String::class.java) ?: ""
                    val rawReplyText = child.child("replyToText").getValue(String::class.java) ?: ""
                    val replyToSender = child.child("replyToSender").getValue(String::class.java) ?: ""

                    val decryptedReplyText = if (rawReplyText.isNotBlank()) {
                        MessageCrypto.decrypt(rawReplyText, chatId)
                    } else {
                        ""
                    }

                    // Decrypt main text or show deleted notice
                    val decryptedContent = when {
                        deletedForEveryone -> "Pesan ini telah dihapus"
                        encryptedText.isNotBlank() -> MessageCrypto.decrypt(encryptedText, chatId)
                        else -> ""
                    }

                    val message = ChatMessage(
                        id = messageId,
                        chatId = chatId,
                        senderId = senderId,
                        senderUsername = senderUsername,
                        senderAvatarColor = senderAvatarColor,
                        encryptedText = encryptedText,
                        decryptedText = decryptedContent,
                        timestamp = timestamp,
                        isEdited = isEdited,
                        deletedForEveryone = deletedForEveryone,
                        replyToMessageId = replyToMessageId,
                        replyToText = decryptedReplyText,
                        replyToSender = replyToSender,
                        reactions = reactionsMap,
                        isPinned = isPinned,
                        forwardedFrom = forwardedFrom
                    )

                    messageList.add(message)
                }

                // Sort chronologically (oldest to newest)
                val sorted = messageList.sortedBy { it.timestamp }
                trySend(sorted)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    /**
     * Sends an AES-256 encrypted message to Firebase Realtime Database.
     */
    fun sendMessage(
        chatId: String,
        sender: User,
        plainText: String,
        replyTo: ChatMessage? = null
    ): Flow<Result<ChatMessage>> = flow {
        try {
            val messageRef = firebaseDatabase.getReference("messages").child(chatId).push()
            val messageId = messageRef.key ?: throw Exception("Gagal membuat ID pesan")

            // Encrypt using AES-256-GCM
            val encryptedPayload = MessageCrypto.encrypt(plainText.trim(), chatId)
            val now = System.currentTimeMillis()

            val messageData = hashMapOf<String, Any>(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to sender.uid,
                "senderUsername" to sender.username,
                "senderAvatarColor" to sender.avatarColorHex,
                "encryptedText" to encryptedPayload,
                "timestamp" to now,
                "isEdited" to false,
                "deletedForEveryone" to false
            )

            // Attach reply quote if replying
            if (replyTo != null) {
                messageData["replyToMessageId"] = replyTo.id
                messageData["replyToText"] = MessageCrypto.encrypt(replyTo.decryptedText, chatId)
                messageData["replyToSender"] = replyTo.senderUsername
            }

            // Multi-path update: write message & update chat preview
            val updates = hashMapOf<String, Any>(
                "/messages/$chatId/$messageId" to messageData,
                "/chats/$chatId/lastMessage" to encryptedPayload,
                "/chats/$chatId/lastMessageTimestamp" to now
            )

            firebaseDatabase.reference.updateChildren(updates).await()

            val chatMessage = ChatMessage(
                id = messageId,
                chatId = chatId,
                senderId = sender.uid,
                senderUsername = sender.username,
                senderAvatarColor = sender.avatarColorHex,
                encryptedText = encryptedPayload,
                decryptedText = plainText.trim(),
                timestamp = now,
                replyToMessageId = replyTo?.id ?: "",
                replyToText = replyTo?.decryptedText ?: "",
                replyToSender = replyTo?.senderUsername ?: ""
            )

            emit(Result.success(chatMessage))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Edits an existing message by re-encrypting the new text and marking it as edited.
     */
    fun editMessage(
        chatId: String,
        messageId: String,
        newPlainText: String
    ): Flow<Result<Boolean>> = flow {
        try {
            val newEncrypted = MessageCrypto.encrypt(newPlainText.trim(), chatId)
            val updates = hashMapOf<String, Any>(
                "/messages/$chatId/$messageId/encryptedText" to newEncrypted,
                "/messages/$chatId/$messageId/isEdited" to true,
                "/messages/$chatId/$messageId/editedTimestamp" to System.currentTimeMillis()
            )

            firebaseDatabase.reference.updateChildren(updates).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Deletes a message for the current user only ("Hapus untuk Saya").
     */
    fun deleteMessageForMe(
        chatId: String,
        messageId: String,
        currentUserId: String
    ): Flow<Result<Boolean>> = flow {
        try {
            firebaseDatabase.getReference("messages")
                .child(chatId)
                .child(messageId)
                .child("deletedForUsers")
                .child(currentUserId)
                .setValue(true)
                .await()

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Deletes a message for all participants ("Hapus untuk Semua").
     */
    fun deleteMessageForEveryone(
        chatId: String,
        messageId: String
    ): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any>(
                "/messages/$chatId/$messageId/deletedForEveryone" to true,
                "/messages/$chatId/$messageId/encryptedText" to ""
            )

            firebaseDatabase.reference.updateChildren(updates).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Adds or removes a reaction emoji (❤️, 👍, 🔥) on a message.
     */
    fun toggleReaction(
        chatId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): Flow<Result<Boolean>> = flow {
        try {
            val reactionRef = firebaseDatabase.getReference("messages")
                .child(chatId)
                .child(messageId)
                .child("reactions")
                .child(userId)

            val currentSnapshot = reactionRef.get().await()
            val currentEmoji = currentSnapshot.getValue(String::class.java)

            if (currentEmoji == emoji) {
                // If same emoji selected again, toggle off (remove reaction)
                reactionRef.removeValue().await()
            } else {
                reactionRef.setValue(emoji).await()
            }

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Pins or unpins a message in the chat room header.
     */
    fun togglePinMessage(
        chatId: String,
        messageId: String,
        currentIsPinned: Boolean
    ): Flow<Result<Boolean>> = flow {
        try {
            val newPinnedState = !currentIsPinned
            val updates = hashMapOf<String, Any?>(
                "/messages/$chatId/$messageId/isPinned" to if (newPinnedState) true else null,
                "/chats/$chatId/pinnedMessageId" to if (newPinnedState) messageId else null
            )

            firebaseDatabase.reference.updateChildren(updates).await()
            emit(Result.success(newPinnedState))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Forwards an existing message to another chat room.
     */
    fun forwardMessage(
        targetChatId: String,
        sender: User,
        plainText: String,
        originalSenderName: String
    ): Flow<Result<ChatMessage>> = flow {
        try {
            val messageRef = firebaseDatabase.getReference("messages").child(targetChatId).push()
            val messageId = messageRef.key ?: throw Exception("Gagal membuat ID pesan teruskan")

            val encryptedPayload = MessageCrypto.encrypt(plainText.trim(), targetChatId)
            val now = System.currentTimeMillis()

            val messageData = hashMapOf<String, Any>(
                "id" to messageId,
                "chatId" to targetChatId,
                "senderId" to sender.uid,
                "senderUsername" to sender.username,
                "senderAvatarColor" to sender.avatarColorHex,
                "encryptedText" to encryptedPayload,
                "timestamp" to now,
                "isEdited" to false,
                "deletedForEveryone" to false,
                "forwardedFrom" to originalSenderName
            )

            val updates = hashMapOf<String, Any>(
                "/messages/$targetChatId/$messageId" to messageData,
                "/chats/$targetChatId/lastMessage" to encryptedPayload,
                "/chats/$targetChatId/lastMessageTimestamp" to now
            )

            firebaseDatabase.reference.updateChildren(updates).await()

            val chatMessage = ChatMessage(
                id = messageId,
                chatId = targetChatId,
                senderId = sender.uid,
                senderUsername = sender.username,
                senderAvatarColor = sender.avatarColorHex,
                encryptedText = encryptedPayload,
                decryptedText = plainText.trim(),
                timestamp = now,
                forwardedFrom = originalSenderName
            )

            emit(Result.success(chatMessage))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

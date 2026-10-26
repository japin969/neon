package app.neon.com.data.repository

import app.neon.com.data.crypto.MessageCrypto
import app.neon.com.data.local.FileDatabaseManager
import app.neon.com.data.model.ChatItem
import app.neon.com.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val localDatabase: FileDatabaseManager
) {
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    /**
     * Observes all chats for the authenticated user in Realtime.
     * Integrates decrypted last message preview, pinned status, and archive folder filter.
     */
    fun observeUserChats(currentUserId: String): Flow<List<ChatItem>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(localDatabase.getCachedChats())
            close()
            return@callbackFlow
        }

        val chatsRef = firebaseDatabase.getReference("chats")
        val userPrefsRef = firebaseDatabase.getReference("users").child(currentUserId)

        // Read user's pinned, archived, and hidden preferences
        var pinnedChatIds = setOf<String>()
        var archivedChatIds = setOf<String>()
        var hiddenChatIds = setOf<String>()

        val userPrefsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pinnedChatIds = snapshot.child("pinnedChats").children
                    .filter { it.getValue(Boolean::class.java) == true }
                    .mapNotNull { it.key }
                    .toSet()

                archivedChatIds = snapshot.child("archivedChats").children
                    .filter { it.getValue(Boolean::class.java) == true }
                    .mapNotNull { it.key }
                    .toSet()

                hiddenChatIds = snapshot.child("hiddenChats").children
                    .filter { it.getValue(Boolean::class.java) == true }
                    .mapNotNull { it.key }
                    .toSet()
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        userPrefsRef.addValueEventListener(userPrefsListener)

        val chatsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatList = mutableListOf<ChatItem>()

                for (child in snapshot.children) {
                    val chatId = child.key ?: continue

                    // Check if current user is hidden from this chat
                    if (hiddenChatIds.contains(chatId)) continue

                    // Check if current user is a participant
                    val participantsSnapshot = child.child("participants")
                    val isParticipant = participantsSnapshot.child(currentUserId).getValue(Boolean::class.java) == true

                    if (!isParticipant) continue

                    val isGroup = child.child("isGroup").getValue(Boolean::class.java) ?: false
                    val chatName = child.child("chatName").getValue(String::class.java) ?: ""
                    val rawLastMessage = child.child("lastMessage").getValue(String::class.java) ?: ""
                    val timestamp = child.child("lastMessageTimestamp").getValue(Long::class.java) ?: 0L
                    val unreadCount = child.child("unread_${currentUserId}").getValue(Int::class.java) ?: 0
                    val pinnedMsgId = child.child("pinnedMessageId").getValue(String::class.java) ?: ""

                    // Decrypt preview if encrypted
                    val displayLastMsg = if (rawLastMessage.isNotBlank()) {
                        MessageCrypto.decrypt(rawLastMessage, chatId)
                    } else {
                        "Belum ada pesan"
                    }

                    // Extract recipient data for 1-on-1 direct chat
                    var recipientId = ""
                    var recipientUsername = ""
                    var recipientAvatarColor = "#00FF66"
                    var recipientAvatarUrl = ""

                    if (!isGroup) {
                        for (pChild in participantsSnapshot.children) {
                            val pUid = pChild.key ?: ""
                            if (pUid != currentUserId && pUid.isNotBlank()) {
                                recipientId = pUid
                                break
                            }
                        }

                        // Read metadata snapshot for recipient if stored in chat
                        val recipientMeta = child.child("participant_meta").child(recipientId)
                        recipientUsername = recipientMeta.child("username").getValue(String::class.java) ?: "User"
                        recipientAvatarColor = recipientMeta.child("avatarColorHex").getValue(String::class.java) ?: "#00E5FF"
                        recipientAvatarUrl = recipientMeta.child("avatarUrl").getValue(String::class.java) ?: ""
                    }

                    val participantsMap = mutableMapOf<String, Boolean>()
                    for (pChild in participantsSnapshot.children) {
                        val pKey = pChild.key ?: continue
                        participantsMap[pKey] = pChild.getValue(Boolean::class.java) ?: true
                    }

                    chatList.add(
                        ChatItem(
                            id = chatId,
                            isGroup = isGroup,
                            chatName = if (isGroup) chatName else recipientUsername,
                            recipientId = recipientId,
                            recipientUsername = recipientUsername,
                            recipientAvatarColor = recipientAvatarColor,
                            recipientAvatarUrl = recipientAvatarUrl,
                            lastMessage = displayLastMsg,
                            lastMessageTimestamp = timestamp,
                            unreadCount = unreadCount,
                            isPinned = pinnedChatIds.contains(chatId),
                            isArchived = archivedChatIds.contains(chatId),
                            participants = participantsMap,
                            pinnedMessageId = pinnedMsgId
                        )
                    )
                }

                // Sort: Pinned first, then by timestamp descending
                val sortedList = chatList.sortedWith(
                    compareByDescending<ChatItem> { it.isPinned }
                        .thenByDescending { it.lastMessageTimestamp }
                )

                // Save to local cache asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    localDatabase.saveCachedChats(sortedList)
                }

                trySend(sortedList)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(localDatabase.getCachedChats())
            }
        }

        chatsRef.addValueEventListener(chatsListener)

        awaitClose {
            chatsRef.removeEventListener(chatsListener)
            userPrefsRef.removeEventListener(userPrefsListener)
        }
    }

    /**
     * Toggles Pinned status for a specific chat for the current user.
     */
    fun togglePinChat(userId: String, chatId: String, currentPinned: Boolean): Flow<Result<Boolean>> = flow {
        try {
            val newPinnedState = !currentPinned
            firebaseDatabase.getReference("users")
                .child(userId)
                .child("pinnedChats")
                .child(chatId)
                .setValue(if (newPinnedState) true else null)
                .await()
            emit(Result.success(newPinnedState))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Toggles Archive status for a specific chat for the current user.
     */
    fun toggleArchiveChat(userId: String, chatId: String, currentArchived: Boolean): Flow<Result<Boolean>> = flow {
        try {
            val newArchivedState = !currentArchived
            firebaseDatabase.getReference("users")
                .child(userId)
                .child("archivedChats")
                .child(chatId)
                .setValue(if (newArchivedState) true else null)
                .await()
            emit(Result.success(newArchivedState))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Deletes a chat from the user's active list (soft delete).
     * The chat history remains preserved in Firebase Realtime Database.
     */
    fun deleteChatFromList(userId: String, chatId: String): Flow<Result<Boolean>> = flow {
        try {
            firebaseDatabase.getReference("users")
                .child(userId)
                .child("hiddenChats")
                .child(chatId)
                .setValue(true)
                .await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Searches a contact by @username in Firebase (/usernames/{username} -> /users/{uid}).
     */
    fun searchContactByUsername(queryUsername: String): Flow<Result<User?>> = callbackFlow {
        val sanitized = queryUsername.trim().lowercase().removePrefix("@")
        if (sanitized.isBlank()) {
            trySend(Result.success(null))
            close()
            return@callbackFlow
        }

        val usernameRef = firebaseDatabase.getReference("usernames").child(sanitized)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val targetUid = snapshot.getValue(String::class.java)
                if (targetUid.isNullOrBlank()) {
                    trySend(Result.success(null))
                    close()
                    return
                }

                // Query full user profile
                val userRef = firebaseDatabase.getReference("users").child(targetUid)
                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        val user = userSnapshot.getValue(User::class.java)
                        trySend(Result.success(user))
                    }

                    override fun onCancelled(error: DatabaseError) {
                        trySend(Result.failure(Exception(error.message)))
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(Exception(error.message)))
            }
        }

        usernameRef.addListenerForSingleValueEvent(listener)
        awaitClose { usernameRef.removeEventListener(listener) }
    }

    /**
     * Creates or retrieves a direct 1-on-1 chat room with another user.
     */
    fun createOrGetDirectChat(currentUser: User, targetUser: User): Flow<Result<ChatItem>> = flow {
        try {
            // Deterministic Direct Chat ID: direct_uid1_uid2
            val uids = listOf(currentUser.uid, targetUser.uid).sorted()
            val chatId = "direct_${uids[0]}_${uids[1]}"

            val chatRef = firebaseDatabase.getReference("chats").child(chatId)
            val snapshot = chatRef.get().await()

            if (!snapshot.exists()) {
                val initialData = hashMapOf<String, Any>(
                    "id" to chatId,
                    "isGroup" to false,
                    "chatName" to targetUser.username,
                    "lastMessage" to MessageCrypto.encrypt("Halo! Mari mulai percakapan di Neont.", chatId),
                    "lastMessageTimestamp" to System.currentTimeMillis(),
                    "participants" to mapOf(
                        currentUser.uid to true,
                        targetUser.uid to true
                    ),
                    "participant_meta" to mapOf(
                        currentUser.uid to mapOf(
                            "username" to currentUser.username,
                            "displayName" to currentUser.displayName,
                            "avatarColorHex" to currentUser.avatarColorHex,
                            "avatarUrl" to currentUser.avatarUrl
                        ),
                        targetUser.uid to mapOf(
                            "username" to targetUser.username,
                            "displayName" to targetUser.displayName,
                            "avatarColorHex" to targetUser.avatarColorHex,
                            "avatarUrl" to targetUser.avatarUrl
                        )
                    )
                )

                chatRef.setValue(initialData).await()
            }

            // Unhide chat for both users in case it was previously deleted from list
            val unhideUpdates = hashMapOf<String, Any?>(
                "/users/${currentUser.uid}/hiddenChats/$chatId" to null,
                "/users/${targetUser.uid}/hiddenChats/$chatId" to null
            )
            firebaseDatabase.reference.updateChildren(unhideUpdates).await()

            val chatItem = ChatItem(
                id = chatId,
                isGroup = false,
                chatName = targetUser.username,
                recipientId = targetUser.uid,
                recipientUsername = targetUser.username,
                recipientAvatarColor = targetUser.avatarColorHex,
                recipientAvatarUrl = targetUser.avatarUrl,
                lastMessage = "Halo! Mari mulai percakapan di Neont.",
                lastMessageTimestamp = System.currentTimeMillis(),
                isPinned = false,
                isArchived = false,
                participants = mapOf(currentUser.uid to true, targetUser.uid to true)
            )

            emit(Result.success(chatItem))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

package app.neon.com.data.repository

import app.neon.com.data.crypto.MessageCrypto
import app.neon.com.data.model.Group
import app.neon.com.data.model.GroupPoll
import app.neon.com.data.model.PollOption
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor() {

    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    /**
     * Creates a new Group with creator as Admin, registers chat room and member metadata.
     */
    fun createGroup(
        name: String,
        avatarColor: String,
        creator: User,
        initialMembers: List<User>
    ): Flow<Result<Group>> = flow {
        try {
            val groupId = "group_${UUID.randomUUID().toString().take(12)}"
            val now = System.currentTimeMillis()

            val allMembers = (listOf(creator) + initialMembers).distinctBy { it.uid }
            val membersMap = allMembers.associate { it.uid to true }
            val adminsMap = mapOf(creator.uid to true)
            val memberProfilesMap = allMembers.associate { it.uid to it }

            val group = Group(
                id = groupId,
                name = name.trim(),
                avatarColor = avatarColor,
                creatorId = creator.uid,
                admins = adminsMap,
                members = membersMap,
                memberProfiles = memberProfilesMap,
                createdAt = now
            )

            // Multi-path atomic write for Group and Chat representation
            val participantMeta = allMembers.associate {
                it.uid to mapOf(
                    "username" to it.username,
                    "displayName" to it.displayName,
                    "avatarColorHex" to it.avatarColorHex,
                    "avatarUrl" to it.avatarUrl
                )
            }

            val updates = hashMapOf<String, Any>(
                "/groups/$groupId" to group,
                "/chats/$groupId" to mapOf(
                    "id" to groupId,
                    "isGroup" to true,
                    "chatName" to name.trim(),
                    "lastMessage" to MessageCrypto.encrypt("Grup dibuat oleh @${creator.username}", groupId),
                    "lastMessageTimestamp" to now,
                    "participants" to membersMap,
                    "participant_meta" to participantMeta
                )
            )

            firebaseDatabase.reference.updateChildren(updates).await()
            emit(Result.success(group))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Updates Group Name and/or Avatar Color.
     */
    fun updateGroupInfo(
        groupId: String,
        newName: String,
        newColor: String
    ): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any>(
                "/groups/$groupId/name" to newName.trim(),
                "/groups/$groupId/avatarColor" to newColor,
                "/chats/$groupId/chatName" to newName.trim()
            )
            firebaseDatabase.reference.updateChildren(updates).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Invites a new member to the group.
     */
    fun inviteMember(groupId: String, user: User): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any>(
                "/groups/$groupId/members/${user.uid}" to true,
                "/groups/$groupId/memberProfiles/${user.uid}" to user,
                "/chats/$groupId/participants/${user.uid}" to true,
                "/chats/$groupId/participant_meta/${user.uid}" to mapOf(
                    "username" to user.username,
                    "displayName" to user.displayName,
                    "avatarColorHex" to user.avatarColorHex,
                    "avatarUrl" to user.avatarUrl
                )
            )
            firebaseDatabase.reference.updateChildren(updates).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Kicks a member from the group (Admin only).
     */
    fun kickMember(groupId: String, memberUid: String): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any?>(
                "/groups/$groupId/members/$memberUid" to null,
                "/groups/$groupId/memberProfiles/$memberUid" to null,
                "/groups/$groupId/admins/$memberUid" to null,
                "/chats/$groupId/participants/$memberUid" to null,
                "/chats/$groupId/participant_meta/$memberUid" to null
            )
            firebaseDatabase.reference.updateChildren(updates).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Pins a group message (Admin only).
     */
    fun pinGroupMessage(groupId: String, messageId: String, text: String): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any>(
                "/groups/$groupId/pinnedMessageId" to messageId,
                "/groups/$groupId/pinnedMessageText" to text,
                "/chats/$groupId/pinnedMessageId" to messageId
            )
            firebaseDatabase.reference.updateChildren(updates).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Creates a group polling (Admin only).
     */
    fun createPoll(
        groupId: String,
        creator: User,
        question: String,
        optionTexts: List<String>
    ): Flow<Result<GroupPoll>> = flow {
        try {
            val pollId = "poll_${UUID.randomUUID().toString().take(10)}"
            val options = optionTexts.mapIndexed { index, text ->
                PollOption(
                    optionId = "opt_$index",
                    text = text.trim(),
                    votes = emptyMap()
                )
            }

            val poll = GroupPoll(
                id = pollId,
                groupId = groupId,
                creatorId = creator.uid,
                creatorUsername = creator.username,
                question = question.trim(),
                options = options,
                isClosed = false,
                createdAt = System.currentTimeMillis()
            )

            firebaseDatabase.getReference("groups")
                .child(groupId)
                .child("polls")
                .child(pollId)
                .setValue(poll)
                .await()

            emit(Result.success(poll))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Casts or toggles a vote on a group poll.
     */
    fun votePoll(
        groupId: String,
        pollId: String,
        optionId: String,
        userId: String
    ): Flow<Result<Boolean>> = flow {
        try {
            val pollRef = firebaseDatabase.getReference("groups")
                .child(groupId)
                .child("polls")
                .child(pollId)

            val snapshot = pollRef.get().await()
            val poll = snapshot.getValue(GroupPoll::class.java) ?: throw Exception("Poll tidak ditemukan")

            // Toggle vote: if already voted on this option, remove; else vote this and unvote others
            val updatedOptions = poll.options.map { option ->
                val newVotes = option.votes.toMutableMap()
                if (option.optionId == optionId) {
                    if (newVotes[userId] == true) {
                        newVotes.remove(userId)
                    } else {
                        newVotes[userId] = true
                    }
                } else {
                    newVotes.remove(userId) // Single-choice poll
                }
                option.copy(votes = newVotes)
            }

            pollRef.child("options").setValue(updatedOptions).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Observes real-time Group details.
     */
    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        if (groupId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val ref = firebaseDatabase.getReference("groups").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val group = snapshot.getValue(Group::class.java)
                trySend(group)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Observes real-time Group polls.
     */
    fun observeGroupPolls(groupId: String): Flow<List<GroupPoll>> = callbackFlow {
        if (groupId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firebaseDatabase.getReference("groups").child(groupId).child("polls")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pollList = mutableListOf<GroupPoll>()
                for (child in snapshot.children) {
                    val poll = child.getValue(GroupPoll::class.java)
                    if (poll != null) {
                        pollList.add(poll)
                    }
                }
                trySend(pollList.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}

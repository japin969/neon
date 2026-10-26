package app.neon.com.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class PollOption(
    val optionId: String = "",
    val text: String = "",
    val votes: Map<String, Boolean> = emptyMap()
) {
    val voteCount: Int get() = votes.size
    fun hasVoted(userId: String): Boolean = votes[userId] == true
}

@IgnoreExtraProperties
data class GroupPoll(
    val id: String = "",
    val groupId: String = "",
    val creatorId: String = "",
    val creatorUsername: String = "",
    val question: String = "",
    val options: List<PollOption> = emptyList(),
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalVotes: Int get() = options.sumOf { it.voteCount }
}

package app.neon.com.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.LeaderboardEntry

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel
) {
    val state by viewModel.uiState.collectAsState()

    val darkBg = Color(0xFF0D0E15)
    val gold = Color(0xFFFFD700)
    val silver = Color(0xFFC0C0C0)
    val bronze = Color(0xFFCD7F32)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141622))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = gold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "LEADERBOARD NEONT",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Pengguna Terpopuler (Pengikut + Total Like)",
                        fontSize = 11.sp,
                        color = Color(0xFF888EA8)
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = gold)
                }
            } else if (state.allEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada data peringkat.",
                        color = Color(0xFF888EA8),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // TOP 3 PODIUM
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Rank 2: Silver
                            state.top2?.let { entry ->
                                PodiumItem(
                                    entry = entry,
                                    rank = 2,
                                    podiumColor = silver,
                                    avatarSize = 64.dp,
                                    elevationHeight = 90.dp
                                )
                            }

                            // Rank 1: Gold
                            state.top1?.let { entry ->
                                PodiumItem(
                                    entry = entry,
                                    rank = 1,
                                    podiumColor = gold,
                                    avatarSize = 80.dp,
                                    elevationHeight = 120.dp
                                )
                            }

                            // Rank 3: Bronze
                            state.top3?.let { entry ->
                                PodiumItem(
                                    entry = entry,
                                    rank = 3,
                                    podiumColor = bronze,
                                    avatarSize = 60.dp,
                                    elevationHeight = 70.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (state.otherEntries.isNotEmpty()) {
                            Text(
                                text = "PERINGKAT LAINNYA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFF7E849B),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    // REST OF LEADERBOARD (Rank 4+)
                    items(state.otherEntries, key = { it.uid }) { entry ->
                        LeaderboardRowItem(entry = entry)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumItem(
    entry: LeaderboardEntry,
    rank: Int,
    podiumColor: Color,
    avatarSize: androidx.compose.ui.unit.Dp,
    elevationHeight: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Avatar Circle with Rank Crown/Badge
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(podiumColor)
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.getInitials(),
                fontSize = if (rank == 1) 28.sp else 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "@${entry.username}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${entry.score} Poin",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = podiumColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Podium Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(elevationHeight)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            podiumColor.copy(alpha = 0.35f),
                            Color(0xFF141726)
                        )
                    )
                )
                .border(
                    1.dp,
                    podiumColor.copy(alpha = 0.5f),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                fontSize = if (rank == 1) 28.sp else 22.sp,
                fontWeight = FontWeight.Black,
                color = podiumColor
            )
        }
    }
}

@Composable
private fun LeaderboardRowItem(entry: LeaderboardEntry) {
    val darkCard = Color(0xFF161824)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(darkCard)
            .border(1.dp, Color(0xFF262A3E), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${entry.rank}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF888EA8),
            modifier = Modifier.width(32.dp)
        )

        val avatarBg = try {
            Color(android.graphics.Color.parseColor(entry.avatarColorHex))
        } catch (_: Exception) {
            Color(0xFF00FF66)
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.getInitials(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "@${entry.username}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${entry.followersCount}",
                    fontSize = 11.sp,
                    color = Color(0xFF888EA8),
                    modifier = Modifier.padding(start = 2.dp, end = 8.dp)
                )

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF007F),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${entry.likesCount}",
                    fontSize = 11.sp,
                    color = Color(0xFF888EA8),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E2336))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${entry.score} pts",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF66)
            )
        }
    }
}

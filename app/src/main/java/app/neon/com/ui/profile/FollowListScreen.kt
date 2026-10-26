package app.neon.com.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.neon.com.data.model.User

@Composable
fun FollowListScreen(
    userId: String,
    initialTab: String,
    viewModel: FollowListViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(userId, initialTab) {
        viewModel.init(userId, initialTab)
    }

    val state by viewModel.uiState.collectAsState()

    val darkBg = Color(0xFF0D0E15)
    val darkCard = Color(0xFF161824)
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)

    val currentTab = state.activeTab
    val userList = if (currentTab == "followers") state.followers else state.following

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141622))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Koneksi Sosial",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Tabs: Pengikut & Mengikuti
            TabRow(
                selectedTabIndex = if (currentTab == "followers") 0 else 1,
                containerColor = Color(0xFF141622),
                contentColor = neonGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(
                            tabPositions[if (currentTab == "followers") 0 else 1]
                        ),
                        color = neonGreen
                    )
                }
            ) {
                Tab(
                    selected = currentTab == "followers",
                    onClick = { viewModel.selectTab("followers") },
                    text = {
                        Text(
                            text = "Pengikut (${state.followers.size})",
                            fontSize = 14.sp,
                            fontWeight = if (currentTab == "followers") FontWeight.Bold else FontWeight.Normal,
                            color = if (currentTab == "followers") neonGreen else Color(0xFF888EA8)
                        )
                    }
                )
                Tab(
                    selected = currentTab == "following",
                    onClick = { viewModel.selectTab("following") },
                    text = {
                        Text(
                            text = "Mengikuti (${state.following.size})",
                            fontSize = 14.sp,
                            fontWeight = if (currentTab == "following") FontWeight.Bold else FontWeight.Normal,
                            color = if (currentTab == "following") neonGreen else Color(0xFF888EA8)
                        )
                    }
                )
            }

            // Content List
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = neonGreen)
                }
            } else if (userList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (currentTab == "followers") "Belum memiliki pengikut." else "Belum mengikuti siapapun.",
                        color = Color(0xFF888EA8),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(userList, key = { it.uid }) { targetUser ->
                        val isFollowing = state.followingUids.contains(targetUser.uid)
                        val isMe = targetUser.uid == state.currentUserId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(darkCard)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val color = try {
                                Color(android.graphics.Color.parseColor(targetUser.avatarColorHex))
                            } catch (_: Exception) {
                                neonCyan
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = targetUser.getInitials(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "@${targetUser.username}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = targetUser.bio,
                                    fontSize = 12.sp,
                                    color = Color(0xFF888EA8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!isMe) {
                                Button(
                                    onClick = { viewModel.toggleFollow(targetUser) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) Color(0xFF24293D) else neonGreen
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isFollowing) "Mengikuti" else "Ikuti",
                                        color = if (isFollowing) Color.White else Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
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

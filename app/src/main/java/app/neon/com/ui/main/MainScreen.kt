package app.neon.com.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.neon.com.ui.chat.ChatListScreen
import app.neon.com.ui.leaderboard.LeaderboardScreen
import app.neon.com.ui.leaderboard.LeaderboardViewModel
import app.neon.com.ui.music.MusicHomeScreen
import app.neon.com.ui.profile.ProfileScreen
import app.neon.com.ui.profile.ProfileViewModel
import app.neon.com.ui.navigation.bottomNavItems
import app.neon.com.ui.viewmodel.MusicViewModel

@Composable
fun MainScreen(
    initialTab: String = "chat",
    onNavigateToChatDetail: (chatId: String, recipientId: String, isGroup: Boolean, chatName: String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToMusicPlayer: () -> Unit,
    onNavigateToStory: (userId: String) -> Unit,
    onNavigateToFollowers: (userId: String, type: String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(initialTab) }

    val darkBg = Color(0xFF0D0E15)
    val darkBarBg = Color(0xFF141622)
    val neonGreen = Color(0xFF00FF66)
    val unselectedText = Color(0xFF7E849B)

    Scaffold(
        containerColor = darkBg,
        bottomBar = {
            NavigationBar(
                containerColor = darkBarBg,
                tonalElevation = 0.dp,
                modifier = Modifier.height(68.dp).border(
                    width = 1.dp,
                    color = Color(0xFF222638),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentTab == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = item.route },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = neonGreen,
                            selectedTextColor = neonGreen,
                            unselectedIconColor = unselectedText,
                            unselectedTextColor = unselectedText,
                            indicatorColor = Color(0x2200FF66)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentTab) {
                "chat" -> {
                    ChatListScreen(
                        onChatClick = { chatId -> onNavigateToChatDetail(chatId, "", false, "") }
                    )
                }
                "music" -> {
                    val musicViewModel: MusicViewModel = hiltViewModel()
                    MusicHomeScreen(musicViewModel = musicViewModel)
                }
                "leaderboard" -> {
                    val leaderboardViewModel: LeaderboardViewModel = hiltViewModel()
                    LeaderboardScreen(viewModel = leaderboardViewModel)
                }
                "profile" -> {
                    val profileViewModel: ProfileViewModel = hiltViewModel()
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onNavigateToStory = onNavigateToStory,
                        onNavigateToFollowers = onNavigateToFollowers,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
        }
    }
}

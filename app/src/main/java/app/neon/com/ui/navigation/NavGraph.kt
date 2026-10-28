package app.neon.com.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import app.neon.com.ui.screens.RoutePlaceholderScreen
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.neon.com.ui.auth.AuthViewModel
import app.neon.com.ui.auth.LoginViewModel
import app.neon.com.ui.auth.RegisterViewModel
import app.neon.com.ui.auth.LoginScreen
import app.neon.com.ui.auth.RegisterScreen
import app.neon.com.ui.call.CallScreen
import app.neon.com.ui.call.CallViewModel
import app.neon.com.ui.chat.ChatDetailScreen
import app.neon.com.ui.chat.ChatDetailViewModel
import app.neon.com.ui.group.CreateGroupScreen
import app.neon.com.ui.group.GroupInfoScreen
import app.neon.com.ui.group.GroupViewModel
import app.neon.com.ui.main.MainScreen
import app.neon.com.ui.video.VideoPlayerScreen
import app.neon.com.ui.music.MusicPlayerScreen
import app.neon.com.ui.video.VideoPlayerScreen
import app.neon.com.ui.music.MusicViewModel
import app.neon.com.ui.profile.DevicesSettingsScreen
import app.neon.com.ui.profile.EditProfileScreen
import app.neon.com.ui.profile.FollowListScreen
import app.neon.com.ui.profile.FollowListViewModel
import app.neon.com.ui.profile.ProfileViewModel
import app.neon.com.ui.profile.SettingsScreen
import app.neon.com.ui.profile.StorageSettingsScreen
import app.neon.com.ui.profile.StoryViewerScreen
import app.neon.com.ui.profile.StoryViewerViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")

    object Main : Screen("main?tab={tab}") {
        fun createRoute(tab: String = "chat"): String = "main?tab=$tab"
    }

    object ChatDetail : Screen("chat/{chatId}?recipientId={recipientId}&isGroup={isGroup}&chatName={chatName}") {
        fun createRoute(
            chatId: String,
            recipientId: String = "",
            isGroup: Boolean = false,
            chatName: String = ""
        ): String = "chat/$chatId?recipientId=$recipientId&isGroup=$isGroup&chatName=$chatName"
    }

    object CreateGroup : Screen("create_group")
    object GroupInfo : Screen("group_info/{groupId}") {
        fun createRoute(groupId: String): String = "group_info/$groupId"
    }

    object Call : Screen("call/{targetUserId}/{targetUserName}/{isVideo}") {
        fun createRoute(
            targetUserId: String,
            targetUserName: String,
            isVideo: Boolean
        ): String = "call/$targetUserId/$targetUserName/$isVideo"
    }

    object StoryViewer : Screen("story/{userId}") {
        fun createRoute(userId: String): String = "story/$userId"
    }

    object FollowList : Screen("follow_list/{userId}?type={type}") {
        fun createRoute(userId: String, type: String = "followers"): String =
            "follow_list/$userId?type=$type"
    }

    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")
    object StorageSettings : Screen("storage_settings")
    object DevicesSettings : Screen("devices_settings")
    object MusicPlayer : Screen("music_player")
    object VideoPlayer : Screen("video_player/{userId}/{initialIndex}") {
        fun createRoute(userId: String, initialIndex: Int = 0): String =
            "video_player/$userId/$initialIndex"
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Chat : BottomNavItem(
        route = "chat",
        title = "Chat",
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    )

    object Music : BottomNavItem(
        route = "music",
        title = "Music",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )

    object Leaderboard : BottomNavItem(
        route = "leaderboard",
        title = "Peringkat",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents
    )

    object Profile : BottomNavItem(
        route = "profile",
        title = "Profil",
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Chat,
    BottomNavItem.Music,
    BottomNavItem.Leaderboard,
    BottomNavItem.Profile
)

class NeontNavigationActions(private val navController: NavHostController) {
    fun navigateToLogin() {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToRegister() {
        navController.navigate(Screen.Register.route) {
            launchSingleTop = true
        }
    }


    fun navigateToMain(tab: String = "chat") {
        navController.navigate(Screen.Main.createRoute(tab)) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToTab(tabRoute: String) {
        navController.navigate(Screen.Main.createRoute(tabRoute)) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToChatDetail(chatId: String, recipientId: String = "", isGroup: Boolean = false, chatName: String = "") {
        navController.navigate(Screen.ChatDetail.createRoute(chatId, recipientId, isGroup, chatName))
    }

    fun navigateToCreateGroup() {
        navController.navigate(Screen.CreateGroup.route)
    }

    fun navigateToGroupInfo(groupId: String) {
        navController.navigate(Screen.GroupInfo.createRoute(groupId))
    }

    fun navigateToCall(targetUserId: String, targetUserName: String, isVideo: Boolean) {
        navController.navigate(Screen.Call.createRoute(targetUserId, targetUserName, isVideo))
    }

    fun navigateToStoryViewer(userId: String) {
        navController.navigate(Screen.StoryViewer.createRoute(userId))
    }

    fun navigateToFollowList(userId: String, type: String) {
        navController.navigate(Screen.FollowList.createRoute(userId, type))
    }

    fun navigateToEditProfile() {
        navController.navigate(Screen.EditProfile.route)
    }

    fun navigateToSettings() {
        navController.navigate(Screen.Settings.route)
    }

    fun navigateToStorageSettings() {
        navController.navigate(Screen.StorageSettings.route)
    }

    fun navigateToDevicesSettings() {
        navController.navigate(Screen.DevicesSettings.route)
    }

    fun navigateToMusicPlayer() {
        navController.navigate(Screen.MusicPlayer.route)
    }

    fun navigateBack() {
        navController.popBackStack()
    }
}

@Composable
fun NeontNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route,
    navigationActions: NeontNavigationActions = remember(navController) { NeontNavigationActions(navController) }
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // 1. Layar Splash
        composable(Screen.Splash.route) {
            RoutePlaceholderScreen(name = "Neont Splash")
        }

        // 2. Layar Login (TAHAP 2)
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = {
                    navigationActions.navigateToRegister()
                },
                onLoginSuccess = {
                    navigationActions.navigateToMain("chat")
                }
            )
        }

        // 3. Layar Registrasi (TAHAP 2)
        composable(Screen.Register.route) {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToLogin = {
                    navigationActions.navigateToLogin()
                },
                onRegisterSuccess = {
                    navigationActions.navigateToMain("chat")
                }
            )
        }

        // 4. Layar Buat Username (TAHAP 2)

        // 5. Layar Utama (Tabs: Chat, Music, Leaderboard, Profil)
        composable(
            route = Screen.Main.route,
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = "chat"
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getString("tab") ?: "chat"
            MainScreen(
                initialTab = initialTab,
                onNavigateToChatDetail = { chatId, recipientId, isGroup, chatName ->
                    navigationActions.navigateToChatDetail(chatId, recipientId, isGroup, chatName)
                },
                onNavigateToCreateGroup = {
                    navigationActions.navigateToCreateGroup()
                },
                onNavigateToMusicPlayer = {
                    navigationActions.navigateToMusicPlayer()
                },
                onNavigateToStory = { userId ->
                    navigationActions.navigateToStoryViewer(userId)
                },
                onNavigateToFollowers = { userId, type ->
                    navigationActions.navigateToFollowList(userId, type)
                },
                onNavigateToEditProfile = {
                    navigationActions.navigateToEditProfile()
                },
                onNavigateToSettings = {
                    navigationActions.navigateToSettings()
                }
            )
        }

        // 6. Layar Detail Chat Enkripsi (TAHAP 4)
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("recipientId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("isGroup") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("chatName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
            val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
            val chatName = backStackEntry.arguments?.getString("chatName") ?: ""
            ChatDetailScreen(
                chatPartnerName = chatName,
                onBackClick = { navigationActions.navigateBack() }
            )
        }

        // 7. Buat Grup (TAHAP 5)
        composable(Screen.CreateGroup.route) {
            val groupViewModel: GroupViewModel = hiltViewModel()
            CreateGroupScreen(
                viewModel = groupViewModel,
                onNavigateBack = { navigationActions.navigateBack() },
                onGroupCreated = { groupId, groupName ->
                    navigationActions.navigateToChatDetail(groupId, "", true, groupName)
                }
            )
        }

        // 8. Info & Admin Grup (TAHAP 5)
        composable(
            route = Screen.GroupInfo.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val groupViewModel: GroupViewModel = hiltViewModel()
            GroupInfoScreen(
                groupId = groupId,
                viewModel = groupViewModel,
                onNavigateBack = { navigationActions.navigateBack() }
            )
        }

        // 9. Voice & Video Call (TAHAP 5)
        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("targetUserId") { type = NavType.StringType },
                navArgument("targetUserName") { type = NavType.StringType },
                navArgument("isVideo") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("targetUserId") ?: ""
            val targetUserName = backStackEntry.arguments?.getString("targetUserName") ?: "User"
            val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false
            val callViewModel: CallViewModel = hiltViewModel()

            CallScreen(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                isVideo = isVideo,
                viewModel = callViewModel,
                onCallEnded = { navigationActions.navigateBack() }
            )
        }

        // 10. Story Viewer (TAHAP 6)
        composable(
            route = Screen.StoryViewer.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val storyViewModel: StoryViewerViewModel = hiltViewModel()
            StoryViewerScreen(
                userId = userId,
                viewModel = storyViewModel,
                onClose = { navigationActions.navigateBack() }
            )
        }

        // 11. Daftar Followers / Following (TAHAP 6)
        composable(
            route = Screen.FollowList.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "followers"
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val type = backStackEntry.arguments?.getString("type") ?: "followers"
            val followViewModel: FollowListViewModel = hiltViewModel()
            FollowListScreen(
                userId = userId,
                initialTab = type,
                viewModel = followViewModel,
                onNavigateBack = { navigationActions.navigateBack() }
            )
        }

        // 12. Edit Profil (TAHAP 6)
        composable(Screen.EditProfile.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            EditProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navigationActions.navigateBack() }
            )
        }

        // 13. Pengaturan (TAHAP 6)
        composable(Screen.Settings.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navigationActions.navigateBack() },
                onNavigateToEditProfile = { navigationActions.navigateToEditProfile() },
                onNavigateToDevices = { navigationActions.navigateToDevicesSettings() },
                onNavigateToStorage = { navigationActions.navigateToStorageSettings() },
                onLoggedOut = { navigationActions.navigateToLogin() }
            )
        }

        // 14. Pengaturan Penyimpanan & Cache (TAHAP 6)
        composable(Screen.StorageSettings.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            StorageSettingsScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navigationActions.navigateBack() }
            )
        }

        // 15. Pengaturan Perangkat Terhubung (TAHAP 6)
        composable(Screen.DevicesSettings.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            DevicesSettingsScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navigationActions.navigateBack() }
            )
        }

        // 16. Spotify-Style Music Player Fullscreen (TAHAP 7)
        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("initialIndex") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0
            val profileViewModel: app.neon.com.ui.profile.ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val state by profileViewModel.uiState.collectAsState()
            
            LaunchedEffect(userId) {
                profileViewModel.loadUserVideos(userId)
            }
            
            VideoPlayerScreen(
                videos = state.userVideos,
                initialIndex = initialIndex,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.MusicPlayer.route) {
            val musicViewModel: MusicViewModel = hiltViewModel()
            MusicPlayerScreen(
                viewModel = musicViewModel,
                onNavigateBack = { navigationActions.navigateBack() }
            )
        }
    }
}

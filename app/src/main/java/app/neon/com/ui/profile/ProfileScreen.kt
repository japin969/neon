package app.neon.com.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToStory: (userId: String) -> Unit,
    onNavigateToFollowers: (userId: String, type: String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val user = state.currentUser
    val scrollState = rememberScrollState()
    var isLikeInfoVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val darkBg = Color(0xFF0D0E15)
    val darkCard = Color(0xFF161824)
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF007F)

    val neonStoryGradient = Brush.sweepGradient(
        listOf(
            Color(0xFF00FF66),
            Color(0xFF00E5FF),
            Color(0xFFFF007F),
            Color(0xFF7928CA),
            Color(0xFF00FF66)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@${user?.username ?: "user"}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color.White
                )

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(darkCard)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Picture with TikTok-Style Story Ring
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(114.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.hasActiveStory) neonStoryGradient else Brush.linearGradient(
                                listOf(Color(0xFF282D44), Color(0xFF282D44))
                            )
                        )
                        .padding(3.dp)
                        .clickable {
                            if (state.hasActiveStory) {
                                onNavigateToStory(user?.uid ?: "")
                            } else {
                                viewModel.openCreateStoryDialog()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarBg = try {
                        Color(android.graphics.Color.parseColor(user?.avatarColorHex ?: "#00FF66"))
                    } catch (_: Exception) {
                        neonGreen
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.getInitials() ?: "NE",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }

                    // Add Story Badge if no active story
                    if (!state.hasActiveStory) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(neonGreen)
                                .border(2.dp, darkBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Story",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = user?.displayName?.ifBlank { user.username } ?: "Pengguna Neont",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = user?.bio ?: "Hey there! I am using Neont.",
                    fontSize = 13.sp,
                    color = Color(0xFF888EA8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // STATISTIK TIKTOK: "Like Out" | "Pengikut" | "Mengikuti"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(darkCard)
                        .border(1.dp, Color(0xFF262B42), RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like Out Counter
                    StatColumn(
                        count = "${user?.likesCount ?: 0}",
                        label = "Like Out",
                        accentColor = neonPink,
                        onClick = { isLikeInfoVisible = true }
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color(0xFF282D42))
                    )

                    // Pengikut (Followers)
                    StatColumn(
                        count = "${user?.followersCount ?: 0}",
                        label = "Pengikut",
                        accentColor = neonCyan,
                        onClick = {
                            onNavigateToFollowers(user?.uid ?: "", "followers")
                        }
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color(0xFF282D42))
                    )

                    // Mengikuti (Following)
                    StatColumn(
                        count = "${user?.followingCount ?: 0}",
                        label = "Mengikuti",
                        accentColor = neonGreen,
                        onClick = {
                            onNavigateToFollowers(user?.uid ?: "", "following")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Edit Profil & Tambah Story
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToEditProfile,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202538)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = neonGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Edit Profil", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.openCreateStoryDialog() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = neonGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Buat Story", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick Menu Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "PENGATURAN CEPAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF7E849B),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                QuickMenuItem(
                    title = "Penyimpanan & Cache",
                    subtitle = "Ukuran Cache: ${state.formattedCacheSize}",
                    onClick = onNavigateToSettings
                )

                Spacer(modifier = Modifier.height(10.dp))

                QuickMenuItem(
                    title = "Perangkat Terhubung (HP)",
                    subtitle = state.loggedInDeviceName,
                    onClick = onNavigateToSettings
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        // Dialog Buat Story Baru
        if (state.isCreateStoryDialogVisible) {
            CreateStoryDialog(
                caption = state.newStoryCaption,
                onCaptionChange = { viewModel.onNewStoryCaptionChanged(it) },
                selectedColor = state.newStoryColor,
                onColorSelect = { viewModel.onNewStoryColorSelected(it) },
                onDismiss = { viewModel.closeCreateStoryDialog() },
                onConfirm = {
                    viewModel.createStory { storyId ->
                        onNavigateToStory(user?.uid ?: "")
                    }
                }
            )
        }

        // Dialog Informasi Like Out
        if (isLikeInfoVisible) {
            AlertDialog(
                onDismissRequest = { isLikeInfoVisible = false },
                containerColor = Color(0xFF161824),
                title = {
                    Text(
                        text = "Tentang Like Out",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Skor Like Out mencerminkan akumulasi total like dan apresiasi yang kamu terima dari pesan chat, profil, dan story di Neont. Skor ini juga mempengaruhi peringkatmu di Leaderboard!",
                        color = Color(0xFFC0C4D6),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { isLikeInfoVisible = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66))
                    ) {
                        Text("Mengerti", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun StatColumn(
    count: String,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = accentColor
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF888EA8)
        )
    }
}

@Composable
private fun QuickMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161824))
            .border(1.dp, Color(0xFF262A3E), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF888EA8))
        }
    }
}

@Composable
private fun CreateStoryDialog(
    caption: String,
    onCaptionChange: (String) -> Unit,
    selectedColor: String,
    onColorSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = listOf("#7928CA", "#00FF66", "#00E5FF", "#FF007F", "#FF9100", "#161824")
    val neonGreen = Color(0xFF00FF66)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161824),
        title = { Text("Buat Story 5 Detik", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tulis status atau pemikiran kamu hari ini:",
                    fontSize = 13.sp,
                    color = Color(0xFF888EA8),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    placeholder = { Text("Apa yang sedang kamu dengarkan / rasakan?") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = neonGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pilih Warna Latar:",
                    fontSize = 12.sp,
                    color = Color(0xFF888EA8),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colors) { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = hex.equals(selectedColor, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { onColorSelect(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = caption.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = neonGreen)
            ) {
                Text("Publikasikan", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color(0xFF888EA8))
            }
        }
    )
}

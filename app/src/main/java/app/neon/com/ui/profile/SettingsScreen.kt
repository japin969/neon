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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val user = state.currentUser
    val scrollState = rememberScrollState()

    val darkBg = Color(0xFF0D0E15)
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)

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
                    text = "Pengaturan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Akun & Sistem
                Text(
                    text = "AKUN & SISTEM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF888EA8),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                SettingsMenuItem(
                    icon = Icons.Default.Edit,
                    iconTint = neonGreen,
                    title = "Edit Profil",
                    subtitle = "Ubah nama, bio, avatar, dan foto profil",
                    onClick = onNavigateToEditProfile
                )

                SettingsMenuItem(
                    icon = Icons.Default.Devices,
                    iconTint = neonCyan,
                    title = "Perangkat Terhubung",
                    subtitle = "HP yang sedang login ke akun ini",
                    onClick = onNavigateToDevices
                )

                SettingsMenuItem(
                    icon = Icons.Default.Storage,
                    iconTint = Color(0xFFFFB703),
                    title = "Penyimpanan & Cache",
                    subtitle = "Kelola database offline & hapus cache",
                    onClick = onNavigateToStorage
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Privasi & Keamanan (Tersimpan ke Firebase)
                Text(
                    text = "PRIVASI & KEAMANAN (REAL-TIME FIREBASE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = neonCyan,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                SettingsToggleItem(
                    icon = Icons.Default.Visibility,
                    iconTint = neonGreen,
                    title = "Terakhir Dilihat (Last Seen)",
                    subtitle = if (user?.showLastSeen != false) "Terlihat oleh semua pengguna" else "Disembunyikan (Privat)",
                    isChecked = user?.showLastSeen != false,
                    onCheckedChange = { viewModel.togglePrivacyLastSeen() }
                )

                SettingsToggleItem(
                    icon = Icons.Default.AccountBox,
                    iconTint = Color(0xFF7928CA),
                    title = "Foto Profil Publik",
                    subtitle = if (user?.showProfilePhoto != false) "Foto dapat dilihat publik" else "Hanya inisial nama",
                    isChecked = user?.showProfilePhoto != false,
                    onCheckedChange = { viewModel.togglePrivacyProfilePhoto() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Logout
                SettingsMenuItem(
                    icon = Icons.Default.Logout,
                    iconTint = Color(0xFFFF0055),
                    title = "Keluar dari Akun",
                    subtitle = "Sign out Firebase & hapus cache lokal",
                    onClick = {
                        viewModel.logout(onLoggedOut)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val neonGreen = Color(0xFF00FF66)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161824))
            .border(1.dp, Color(0xFF262A3E), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF888EA8))
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = neonGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF282D42)
            )
        )
    }
}

@Composable
private fun SettingsMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161824))
            .border(1.dp, Color(0xFF262A3E), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF888EA8))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color(0xFF5D6377),
            modifier = Modifier.size(18.dp)
        )
    }
}

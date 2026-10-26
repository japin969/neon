package app.neon.com.ui.group

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateGroupScreen(
    viewModel: GroupViewModel,
    onNavigateBack: () -> Unit,
    onGroupCreated: (groupId: String, groupName: String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val darkBg = Color(0xFF0D0E15)
    val darkCard = Color(0xFF161824)
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)

    val neonColorOptions = listOf(
        "#00FF66",
        "#00E5FF",
        "#FF007F",
        "#7928CA",
        "#FF9100",
        "#F72585",
        "#00F5D4"
    )

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
                    text = "Buat Grup Baru",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Header Profile & Name Input
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(darkCard)
                            .border(1.dp, Color(0xFF262A3E), RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val currentColor = try {
                            Color(android.graphics.Color.parseColor(state.selectedColor))
                        } catch (_: Exception) {
                            neonGreen
                        }

                        // Avatar Preview
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(currentColor)
                                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Color Picker
                        Text(
                            text = "Pilih Warna Aksen Grup:",
                            fontSize = 12.sp,
                            color = Color(0xFF888EA8),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(neonColorOptions) { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = hex.equals(state.selectedColor, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.onColorSelected(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Nama Grup Input
                        OutlinedTextField(
                            value = state.groupNameInput,
                            onValueChange = { viewModel.onGroupNameChanged(it) },
                            label = { Text("Nama Grup Neont", color = Color(0xFF888EA8)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = neonGreen,
                                unfocusedBorderColor = Color(0xFF262A3E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = neonGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "PILIH ANGGOTA AWAL (${state.selectedMembers.size} Dipilih)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = neonCyan,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (state.availableContacts.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada kontak langsung. Kamu dapat mengundang anggota nanti di info grup.",
                            fontSize = 13.sp,
                            color = Color(0xFF888EA8),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(state.availableContacts) { contact ->
                        val isSelected = state.selectedMembers.any { it.uid == contact.uid }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(darkCard)
                                .clickable { viewModel.toggleMemberSelection(contact) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(neonCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.getInitials(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "@${contact.username}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleMemberSelection(contact) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = neonGreen,
                                    checkmarkColor = Color.Black,
                                    uncheckedColor = Color(0xFF4B5563)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Bottom Confirm Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141622))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.createGroup(onGroupCreated)
                    },
                    enabled = state.groupNameInput.isNotBlank() && !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = neonGreen,
                        disabledContainerColor = Color(0xFF1E2F26)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "BUAT GRUP NEONT",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

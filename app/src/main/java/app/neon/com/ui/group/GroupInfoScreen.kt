package app.neon.com.ui.group

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.GroupPoll

@Composable
fun GroupInfoScreen(
    groupId: String,
    viewModel: GroupViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(groupId) {
        viewModel.initGroup(groupId)
    }

    val state by viewModel.uiState.collectAsState()
    val group = state.group
    val currentUser = state.currentUser
    val isAdmin = group?.isAdmin(currentUser?.uid ?: "") == true

    val darkBg = Color(0xFF0D0E15)
    val darkCard = Color(0xFF161824)
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF007F)

    var editNameInput by remember(group?.name) { mutableStateOf(group?.name ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        if (group == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = neonGreen)
            }
        } else {
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
                        text = "Info Grup",
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
                    // Header Grup Info
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
                            val color = try {
                                Color(android.graphics.Color.parseColor(group.avatarColor))
                            } catch (_: Exception) {
                                neonGreen
                            }

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = group.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (isAdmin) {
                                    IconButton(
                                        onClick = { viewModel.openEditGroupDialog() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Nama",
                                            tint = neonGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${group.members.size} Anggota",
                                fontSize = 13.sp,
                                color = Color(0xFF888EA8),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Admin Action Bar
                        if (isAdmin) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.openInviteDialog() },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = neonCyan)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Undang", color = Color.White, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { viewModel.openCreatePollDialog() },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.HowToVote, contentDescription = null, tint = neonGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Buat Polling", color = Color.White, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Polling Section
                        if (state.polls.isNotEmpty()) {
                            Text(
                                text = "POLLING AKTIF (${state.polls.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = neonGreen,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    // Poll Cards
                    items(state.polls, key = { it.id }) { poll ->
                        PollCard(
                            poll = poll,
                            currentUserId = currentUser?.uid ?: "",
                            onVote = { optId ->
                                viewModel.votePoll(poll.id, optId)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Anggota Section Header
                    item {
                        Text(
                            text = "DAFTAR ANGGOTA (${group.members.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = neonCyan,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }

                    // Member Items
                    val memberList = group.members.keys.toList()
                    items(memberList, key = { it }) { memberUid ->
                        val memberUser = group.memberProfiles[memberUid]
                        val isMemberAdmin = group.isAdmin(memberUid)
                        val canKick = isAdmin && !isMemberAdmin && memberUid != currentUser?.uid

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(darkCard)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(neonCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = memberUser?.getInitials() ?: "U",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "@${memberUser?.username ?: memberUid.take(8)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (isMemberAdmin) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x3300FF66))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Admin",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = neonGreen
                                            )
                                        }
                                    }
                                }
                            }

                            if (canKick) {
                                IconButton(
                                    onClick = { viewModel.kickMember(memberUid) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonRemove,
                                        contentDescription = "Keluarkan",
                                        tint = Color(0xFFFF4D6D),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Dialog Edit Nama Grup
        if (state.isEditGroupDialogVisible && group != null) {
            AlertDialog(
                onDismissRequest = { viewModel.closeEditGroupDialog() },
                containerColor = darkCard,
                title = { Text("Edit Nama Grup", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Nama Baru") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = neonGreen,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateGroupInfo(editNameInput, group.avatarColor)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = neonGreen)
                    ) {
                        Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeEditGroupDialog() }) {
                        Text("Batal", color = Color(0xFF888EA8))
                    }
                }
            )
        }

        // Dialog Undang Anggota
        if (state.isInviteDialogVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.closeInviteDialog() },
                containerColor = darkCard,
                title = { Text("Undang Anggota", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = state.inviteUsernameQuery,
                            onValueChange = { viewModel.onInviteUsernameChanged(it) },
                            placeholder = { Text("@username", color = Color(0xFF6B7280)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = neonCyan,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.inviteError != null) {
                            Text(
                                text = state.inviteError ?: "",
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.inviteMember() },
                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
                    ) {
                        Text("Undang", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeInviteDialog() }) {
                        Text("Batal", color = Color(0xFF888EA8))
                    }
                }
            )
        }

        // Dialog Buat Polling (Admin Only)
        if (state.isCreatePollDialogVisible) {
            CreatePollDialog(
                onDismiss = { viewModel.closeCreatePollDialog() },
                onCreate = { question, options ->
                    viewModel.createPoll(question, options)
                }
            )
        }
    }
}

@Composable
private fun PollCard(
    poll: GroupPoll,
    currentUserId: String,
    onVote: (optionId: String) -> Unit
) {
    val darkCard = Color(0xFF1B1E2D)
    val neonGreen = Color(0xFF00FF66)
    val total = if (poll.totalVotes > 0) poll.totalVotes else 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(darkCard)
            .border(1.dp, Color(0xFF282D44), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.HowToVote, contentDescription = null, tint = neonGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = poll.question,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = "Dibuat oleh @${poll.creatorUsername} • Total ${poll.totalVotes} Suara",
            fontSize = 11.sp,
            color = Color(0xFF888EA8),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        poll.options.forEach { option ->
            val hasVoted = option.hasVoted(currentUserId)
            val percentage = (option.voteCount.toFloat() / total)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141724))
                    .border(
                        1.dp,
                        if (hasVoted) neonGreen else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onVote(option.optionId) }
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = option.text,
                        fontSize = 13.sp,
                        fontWeight = if (hasVoted) FontWeight.Bold else FontWeight.Normal,
                        color = if (hasVoted) neonGreen else Color.White
                    )
                    Text(
                        text = "${option.voteCount} (${(percentage * 100).toInt()}%)",
                        fontSize = 12.sp,
                        color = Color(0xFF888EA8)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = neonGreen,
                    trackColor = Color(0xFF222638)
                )
            }
        }
    }
}

@Composable
private fun CreatePollDialog(
    onDismiss: () -> Unit,
    onCreate: (question: String, options: List<String>) -> Unit
) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    val neonGreen = Color(0xFF00FF66)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161824),
        title = { Text("Buat Polling Baru", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Pertanyaan Polling") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = neonGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                options.forEachIndexed { index, optionText ->
                    OutlinedTextField(
                        value = optionText,
                        onValueChange = { options[index] = it },
                        label = { Text("Opsi ${index + 1}") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = neonGreen,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                if (options.size < 5) {
                    TextButton(onClick = { options.add("") }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = neonGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Opsi", color = neonGreen)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(question, options.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = neonGreen)
            ) {
                Text("Buat", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color(0xFF888EA8))
            }
        }
    )
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.LiveStream
import com.example.core.repository.CineCutRepository
import com.example.ui.components.CineCoinBadge
import com.example.ui.components.CineCoinStatusBadge
import com.example.ui.components.M3u8PlayerView
import com.example.ui.components.MembershipBadge
import com.example.ui.components.UserCoinTag
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineLiveScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveStreams by repository.liveStreams.collectAsState()
    val liveChat by repository.liveChat.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    val currentStream = liveStreams.firstOrNull() ?: LiveStream(
        creatorId = "usr_creator",
        creatorName = "David Kim",
        title = "🔴 LIVE: Editing A Sci-Fi Trailer in CineCut",
        hlsStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    )

    var chatMessageInput by remember { mutableStateOf("") }
    var showTipDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(CinePrimary)
                        )
                        Text("CineLive", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    CineCoinBadge(balance = currentUser.coinBalance)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineBackground)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("cine_live_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Video Stream Viewport with Real M3U8 Player
            M3u8PlayerView(
                m3u8Url = currentStream.hlsStreamUrl,
                isLiveStream = true,
                autoPlay = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            )

            // Stream Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineSurface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentStream.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(currentStream.creatorName, fontSize = 11.sp, color = CineTertiary)
                        VerifiedBadge()
                    }
                }

                // Tip Creator Button
                Button(
                    onClick = { showTipDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("tip_creator_button")
                ) {
                    Icon(imageVector = Icons.Default.Paid, contentDescription = null, tint = Color(0xFF332000), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tip ¢", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF332000))
                }
            }

            Divider(color = CineTimelineRuler, thickness = 1.dp)

            // Live Chat Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(liveChat) { chat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (chat.coinTip > 0) CineSecondary.copy(alpha = 0.15f) else CineSurface)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MembershipBadge(tier = chat.senderTier)
                        CineCoinStatusBadge(coinType = chat.senderCoinType, size = 15.dp)
                        Text(chat.senderName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                        Text(chat.message, fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f))

                        if (chat.coinTip > 0) {
                            Text("+${chat.coinTip}¢", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineSecondary)
                        }
                    }
                }
            }

            // Chat Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatMessageInput,
                    onValueChange = { chatMessageInput = it },
                    placeholder = { Text("Chat live...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CinePrimary,
                        unfocusedBorderColor = CineTimelineRuler,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                IconButton(
                    onClick = {
                        if (chatMessageInput.isNotBlank()) {
                            repository.sendLiveChatMessage(chatMessageInput, 0L)
                            chatMessageInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(CinePrimary)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Tipping Dialog
    if (showTipDialog) {
        AlertDialog(
            onDismissRequest = { showTipDialog = false },
            containerColor = CineSurface,
            title = { Text("Send CineCoins to Creator", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select tip amount. Funds are transferred via backend ledger.", fontSize = 12.sp, color = CineTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50L, 100L, 500L, 1000L).forEach { amount ->
                            Button(
                                onClick = {
                                    repository.sendLiveChatMessage("Sent a ${amount}¢ Tip! 🎬✨", amount)
                                    showTipDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${amount}¢", color = Color(0xFF332000), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTipDialog = false }) {
                    Text("Close", color = CineTextSecondary)
                }
            }
        )
    }
}

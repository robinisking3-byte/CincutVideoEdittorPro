package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * User Support Interface:
 * Profile → Help & Support → Create Ticket
 *
 * Full lifecycle support ticket system for CineCut creators:
 * - View tickets & status
 * - Create ticket with category, priority, attachments & order ID
 * - Real-time chat stream with support agents
 * - Resolution, closing & reopening workflows
 * - Strictly isolates internal admin notes from user view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val allTickets by repository.supportTickets.collectAsState()
    val ticketMessages by repository.ticketMessages.collectAsState()
    val paymentOrders by repository.paymentOrders.collectAsState()

    var selectedTicketId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: My Tickets, 1: Help Topics / FAQ

    // Filter tickets belonging to logged in user
    val userTickets = remember(allTickets, currentUser.uid) {
        allTickets.filter { it.userId == currentUser.uid }
    }

    val selectedTicket = remember(allTickets, selectedTicketId) {
        allTickets.find { it.ticketId == selectedTicketId }
    }

    // Handle back navigation when inspecting a ticket
    BackHandler {
        if (selectedTicketId != null) {
            selectedTicketId = null
        } else {
            onBack()
        }
    }

    if (selectedTicket != null) {
        // TICKET DETAIL & CONVERSATION VIEW
        val messages = ticketMessages[selectedTicket!!.ticketId] ?: emptyList()
        // Filter out internal notes — internal notes are strictly admin-only
        val userVisibleMessages = messages.filter { !it.isInternalNote }

        UserTicketDetailScreen(
            ticket = selectedTicket!!,
            messages = userVisibleMessages,
            currentUserId = currentUser.uid,
            onBack = { selectedTicketId = null },
            onSendMessage = { text, attachments ->
                repository.sendSupportMessage(selectedTicket!!.ticketId, text, isInternalNote = false, attachments = attachments)
            },
            onUpdateStatus = { newStatus ->
                repository.updateSupportTicketStatus(selectedTicket!!.ticketId, newStatus)
            }
        )
    } else {
        // HELP & SUPPORT DASHBOARD / TICKETS LIST
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Help & Support",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Customer care & ticket assistance",
                                fontSize = 11.sp,
                                color = CineTextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = CinePrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("create_ticket_header_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = CinePrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                    text = { Text("Create Ticket", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("create_ticket_fab")
                )
            },
            containerColor = CineBackground,
            modifier = modifier.testTag("support_screen")
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Unread reply notification banner if any ticket has unread support messages
                val unreadCount = userTickets.sumOf { it.unreadUserCount }
                if (unreadCount > 0) {
                    Surface(
                        color = Color(0xFF0C2E42),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = CineTertiary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Support Replied to Your Ticket!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "You have $unreadCount new message(s) from CineCut customer care.",
                                    fontSize = 11.sp,
                                    color = CineTertiary
                                )
                            }
                        }
                    }
                }

                // Sub Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = CineSurface,
                    contentColor = CinePrimary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("My Tickets", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (userTickets.isNotEmpty()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (unreadCount > 0) CinePrimary else CineSurfaceHighlight
                                    ) {
                                        Text(
                                            text = "${userTickets.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("FAQ & Guides", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }

                when (activeTab) {
                    0 -> {
                        // TICKETS LIST
                        if (userTickets.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = CineSurfaceHighlight,
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.SupportAgent,
                                                contentDescription = null,
                                                tint = CineTertiary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "No Support Tickets Yet",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "Have questions about payments, CineCoins, 4K rendering or memberships? Open a ticket to speak with our support staff.",
                                        fontSize = 12.sp,
                                        color = CineTextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create Ticket", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(userTickets) { ticket ->
                                    UserTicketCard(
                                        ticket = ticket,
                                        onClick = {
                                            selectedTicketId = ticket.ticketId
                                            repository.markTicketReadForUser(ticket.ticketId)
                                        }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(72.dp))
                                }
                            }
                        }
                    }

                    1 -> {
                        // FAQ & SELF HELP TOPICS
                        UserFaqSection(
                            onCreateTicketWithCategory = { cat ->
                                showCreateDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // CREATE TICKET DIALOG
    if (showCreateDialog) {
        CreateSupportTicketDialog(
            recentOrders = paymentOrders.filter { it.userId == currentUser.uid },
            onDismiss = { showCreateDialog = false },
            onSubmit = { subject, category, description, priority, orderId, attachments ->
                val newTicket = repository.createSupportTicket(
                    subject = subject,
                    category = category,
                    description = description,
                    priority = priority,
                    orderId = orderId,
                    attachments = attachments
                )
                showCreateDialog = false
                selectedTicketId = newTicket.ticketId
            }
        )
    }
}

/**
 * Individual Ticket Card in Creator List
 */
@Composable
fun UserTicketCard(
    ticket: SupportTicket,
    onClick: () -> Unit
) {
    val statusColor = when (ticket.status) {
        SupportTicketStatus.OPEN -> CinePrimary
        SupportTicketStatus.IN_PROGRESS -> CineSecondary
        SupportTicketStatus.WAITING_FOR_USER -> CineTertiary
        SupportTicketStatus.RESOLVED -> CineSuccess
        SupportTicketStatus.CLOSED -> CineTextTertiary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (ticket.unreadUserCount > 0) CineTertiary else CineTimelineRuler
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_ticket_${ticket.ticketId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        ticket.ticketId,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTertiary
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CineSurfaceHighlight
                    ) {
                        Text(
                            text = ticket.category.title,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (ticket.unreadUserCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = CinePrimary
                        ) {
                            Text(
                                "NEW REPLY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor)
                ) {
                    Text(
                        text = ticket.status.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                ticket.subject,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                ticket.description,
                fontSize = 11.sp,
                color = CineTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (ticket.orderId != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = CineSecondary, modifier = Modifier.size(12.dp))
                    Text("Order: ${ticket.orderId}", fontSize = 10.sp, color = CineSecondary, fontWeight = FontWeight.Medium)
                }
            }

            Divider(color = CineTimelineRuler, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                Text(
                    "Updated: ${dateFormat.format(Date(ticket.lastMessageAt))}",
                    fontSize = 10.sp,
                    color = CineTextTertiary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (ticket.priority) {
                            SupportTicketPriority.URGENT -> CineError.copy(alpha = 0.2f)
                            SupportTicketPriority.HIGH -> CineWarning.copy(alpha = 0.2f)
                            else -> CineSurfaceVariant
                        }
                    ) {
                        Text(
                            text = ticket.priority.name,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (ticket.priority) {
                                SupportTicketPriority.URGENT -> CineError
                                SupportTicketPriority.HIGH -> CineWarning
                                else -> CineTextTertiary
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CineTextTertiary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Ticket Details & Conversation Interface for Users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTicketDetailScreen(
    ticket: SupportTicket,
    messages: List<SupportTicketMessage>,
    currentUserId: String,
    onBack: () -> Unit,
    onSendMessage: (String, List<SupportAttachment>) -> Unit,
    onUpdateStatus: (SupportTicketStatus) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var replyText by remember { mutableStateOf("") }
    var selectedAttachments by remember { mutableStateOf<List<SupportAttachment>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(ticket.ticketId, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (ticket.status) {
                                    SupportTicketStatus.OPEN -> CinePrimary.copy(alpha = 0.2f)
                                    SupportTicketStatus.IN_PROGRESS -> CineSecondary.copy(alpha = 0.2f)
                                    SupportTicketStatus.WAITING_FOR_USER -> CineTertiary.copy(alpha = 0.2f)
                                    SupportTicketStatus.RESOLVED -> CineSuccess.copy(alpha = 0.2f)
                                    SupportTicketStatus.CLOSED -> CineTextTertiary.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    ticket.status.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (ticket.status) {
                                        SupportTicketStatus.OPEN -> CinePrimary
                                        SupportTicketStatus.IN_PROGRESS -> CineSecondary
                                        SupportTicketStatus.WAITING_FOR_USER -> CineTertiary
                                        SupportTicketStatus.RESOLVED -> CineSuccess
                                        SupportTicketStatus.CLOSED -> CineTextTertiary
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            ticket.subject,
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(ticket.ticketId)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Ticket ID", tint = CineTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status and Action Bar
            Surface(
                color = CineSurfaceHighlight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Category: ${ticket.category.title}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        if (ticket.assignedToName != null) {
                            Text("Handled by: ${ticket.assignedToName}", fontSize = 10.sp, color = CineTertiary)
                        } else {
                            Text("Assigned: Support Queue", fontSize = 10.sp, color = CineTextSecondary)
                        }
                    }

                    // Ticket Resolution / Reopen Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (ticket.status == SupportTicketStatus.RESOLVED) {
                            OutlinedButton(
                                onClick = { onUpdateStatus(SupportTicketStatus.OPEN) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineWarning)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(12.dp), tint = CineWarning)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reopen", fontSize = 10.sp, color = CineWarning)
                            }

                            Button(
                                onClick = { onUpdateStatus(SupportTicketStatus.CLOSED) },
                                colors = ButtonDefaults.buttonColors(containerColor = CineSurfaceVariant),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Close", fontSize = 10.sp, color = CineTextSecondary)
                            }
                        } else if (ticket.status != SupportTicketStatus.CLOSED) {
                            OutlinedButton(
                                onClick = { onUpdateStatus(SupportTicketStatus.RESOLVED) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineSuccess)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = CineSuccess)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Resolved", fontSize = 10.sp, color = CineSuccess)
                            }
                        }
                    }
                }
            }

            // Linked Order Bar if present
            if (ticket.orderId != null) {
                Surface(
                    color = Color(0xFF241C0A),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CineSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = CineSecondary, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Referenced Payment Order: ${ticket.orderId}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CineSecondary
                        )
                    }
                }
            }

            // Chat Messages Stream
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    val isMyMessage = msg.senderId == currentUserId || msg.senderRole == "USER"
                    UserChatMessageItem(msg = msg, isMine = isMyMessage)
                }
            }

            // Message Composer (disabled if ticket is closed)
            if (ticket.status == SupportTicketStatus.CLOSED) {
                Surface(
                    color = CineSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CineTextTertiary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("This ticket is closed. Open a new ticket if you need further help.", fontSize = 11.sp, color = CineTextSecondary)
                    }
                }
            } else {
                Surface(
                    color = CineSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Display staged attachments
                        if (selectedAttachments.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(selectedAttachments) { att ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CineSurfaceHighlight,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(12.dp))
                                            Text(att.fileName, fontSize = 10.sp, color = Color.White)
                                            IconButton(
                                                onClick = { selectedAttachments = selectedAttachments.filterNot { it.id == att.id } },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = CineTextSecondary, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Attachment Shortcut Button
                            IconButton(
                                onClick = {
                                    val newAtt = SupportAttachment(
                                        fileName = "screenshot_${System.currentTimeMillis().toString().takeLast(4)}.png",
                                        fileUrl = "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=500",
                                        fileType = "image/png",
                                        fileSizeBytes = 184500L
                                    )
                                    selectedAttachments = selectedAttachments + newAtt
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CineSurfaceHighlight)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Screenshot", tint = CineTertiary)
                            }

                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text("Reply to CineCut support...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("user_support_reply_field"),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CinePrimary,
                                    unfocusedBorderColor = CineTimelineRuler,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                maxLines = 4
                            )

                            IconButton(
                                onClick = {
                                    if (replyText.isNotBlank() || selectedAttachments.isNotEmpty()) {
                                        onSendMessage(replyText, selectedAttachments)
                                        replyText = ""
                                        selectedAttachments = emptyList()
                                    }
                                },
                                enabled = replyText.isNotBlank() || selectedAttachments.isNotEmpty(),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (replyText.isNotBlank() || selectedAttachments.isNotEmpty()) CinePrimary else CineSurfaceVariant)
                                    .testTag("send_user_support_reply_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chat bubble in User Conversation View
 */
@Composable
fun UserChatMessageItem(
    msg: SupportTicketMessage,
    isMine: Boolean
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        // Sender Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            if (!isMine) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(12.dp))
            }
            Text(
                text = if (isMine) "You" else msg.senderName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMine) CinePrimary else CineTertiary
            )
            Text("•", fontSize = 10.sp, color = CineTextTertiary)
            Text(dateFormat.format(Date(msg.createdAt)), fontSize = 9.sp, color = CineTextTertiary)
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMine) 14.dp else 2.dp,
                bottomEnd = if (isMine) 2.dp else 14.dp
            ),
            color = if (isMine) CinePrimary.copy(alpha = 0.15f) else CineSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isMine) CinePrimary.copy(alpha = 0.5f) else CineTimelineRuler
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = msg.message,
                    fontSize = 13.sp,
                    color = Color.White
                )

                // Attachments preview
                if (msg.attachments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        msg.attachments.forEach { att ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CineSurfaceHighlight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(att.fileName, fontSize = 10.sp, color = Color.White, maxLines = 1)
                                        Text("${att.fileSizeBytes / 1024} KB", fontSize = 8.sp, color = CineTextTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * FAQ & Self Help Topics
 */
@Composable
fun UserFaqSection(
    onCreateTicketWithCategory: (SupportTicketCategory) -> Unit
) {
    val faqs = listOf(
        Triple(
            "ZapUPI Payment Deducted but CineCoins Not Received?",
            "UPI transactions usually confirm within 2 minutes. If your bank debited funds but your balance did not update, grab your 12-digit UTR reference from your banking app and submit a Payment support ticket. Our team verifies bank logs immediately.",
            SupportTicketCategory.PAYMENT
        ),
        Triple(
            "How do I export in 4K 60fps ProRes / H.265?",
            "Go to the Video Editor timeline, tap the Export button in the top right, select 4K Ultra HD (3840x2160), set framerate to 60fps, and choose H.265 / HEVC. For HDR, ensure Rec.2020 PQ color space is enabled.",
            SupportTicketCategory.VIDEO_EDITOR
        ),
        Triple(
            "How do CineRooms tier gates and invites work?",
            "Gold, Diamond, and VIP members can host up to 1080p and 4K collaborative review rooms with up to 15 Mbps bitrate. Free members can join any room they are invited to via room link.",
            SupportTicketCategory.CINEROOMS
        ),
        Triple(
            "Upgrading or Cancelling CineCut Membership",
            "You can manage your membership tier at Profile → Manage Membership & Subscription. Upgrades take effect immediately with pro-rated billing.",
            SupportTicketCategory.MEMBERSHIP
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("FREQUENTLY ASKED QUESTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        items(faqs) { (q, a, cat) ->
            var expanded by remember { mutableStateOf(false) }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(q, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = CineTextSecondary
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(a, fontSize = 12.sp, color = CineTextSecondary)
                            OutlinedButton(
                                onClick = { onCreateTicketWithCategory(cat) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary.copy(alpha = 0.5f))
                            ) {
                                Text("Still Need Help? Open ${cat.title} Ticket", fontSize = 10.sp, color = CineTertiary)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

/**
 * Dialog to Open New Support Ticket
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSupportTicketDialog(
    recentOrders: List<ZapUpiOrder>,
    onDismiss: () -> Unit,
    onSubmit: (String, SupportTicketCategory, String, SupportTicketPriority, String?, List<SupportAttachment>) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SupportTicketCategory.PAYMENT) }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(SupportTicketPriority.MEDIUM) }
    var orderIdInput by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<SupportAttachment>>(emptyList()) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Create Support Ticket", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Text("A unique ticket ID will be automatically generated", fontSize = 11.sp, color = CineTextSecondary)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Picker
                item {
                    Text("Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CineTextTertiary)
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.title,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("ticket_category_dropdown"),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            SupportTicketCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cat.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(cat.description, fontSize = 10.sp, color = CineTextSecondary)
                                        }
                                    },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Subject
                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject (e.g. UTR Verification, 4K Export)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ticket_subject_input"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                // Priority Selection
                item {
                    Text("Priority", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CineTextTertiary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SupportTicketPriority.values().forEach { prio ->
                            FilterChip(
                                selected = selectedPriority == prio,
                                onClick = { selectedPriority = prio },
                                label = { Text(prio.name, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (prio) {
                                        SupportTicketPriority.URGENT -> CineError
                                        SupportTicketPriority.HIGH -> CineWarning
                                        SupportTicketPriority.MEDIUM -> CineSecondary
                                        SupportTicketPriority.LOW -> CineTimelineRuler
                                    }
                                )
                            )
                        }
                    }
                }

                // Payment / Order ID (when category is Payment or CineCoins)
                if (selectedCategory == SupportTicketCategory.PAYMENT || selectedCategory == SupportTicketCategory.CINECOINS || selectedCategory == SupportTicketCategory.MEMBERSHIP) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = orderIdInput,
                                onValueChange = { orderIdInput = it },
                                label = { Text("Order ID / Payment Reference (Optional)") },
                                placeholder = { Text("e.g. ZAP_CC_61902847") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ticket_order_id_input"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            if (recentOrders.isNotEmpty()) {
                                Text("Recent Orders:", fontSize = 10.sp, color = CineTextTertiary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(recentOrders) { order ->
                                        SuggestionChip(
                                            onClick = { orderIdInput = order.orderId },
                                            label = { Text("${order.orderId} (₹%.0f)".format(order.amountInr), fontSize = 9.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Details") },
                        placeholder = { Text("Describe what happened, any error messages, device specs, or transaction details...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("ticket_description_input"),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 6
                    )
                }

                // Attachments
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Attachments (${attachments.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CineTextTertiary)
                        TextButton(
                            onClick = {
                                val att = SupportAttachment(
                                    fileName = "screen_issue_${System.currentTimeMillis().toString().takeLast(4)}.png",
                                    fileUrl = "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=500",
                                    fileType = "image/png",
                                    fileSizeBytes = 210000L
                                )
                                attachments = attachments + att
                            }
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Attach Screenshot", fontSize = 11.sp)
                        }
                    }

                    if (attachments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            attachments.forEach { att ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CineSurfaceHighlight,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(att.fileName, fontSize = 10.sp, color = Color.White)
                                        IconButton(
                                            onClick = { attachments = attachments.filterNot { it.id == att.id } },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = CineTextSecondary, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && description.isNotBlank()) {
                        onSubmit(
                            subject,
                            selectedCategory,
                            description,
                            selectedPriority,
                            orderIdInput.ifBlank { null },
                            attachments
                        )
                    }
                },
                enabled = subject.isNotBlank() && description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                modifier = Modifier.testTag("submit_ticket_button")
            ) {
                Text("Submit Ticket", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = CineSurface
    )
}

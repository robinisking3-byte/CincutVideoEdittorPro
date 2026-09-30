package com.example.ui.screens.admin

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
 * Admin Support Center:
 * Admin Panel → Support Tickets
 *
 * Comprehensive support management interface for staff & administrators:
 * - Real-time support dashboard with KPIs & resolution metrics
 * - All Tickets, Assigned to Me, and Unassigned Queue
 * - Search by Ticket ID (#CC-...), Username, Email, Order ID
 * - Multi-dimensional filtering by Status, Category, Priority
 * - Ticket assignment to support staff
 * - Status & Priority management with immutable audit logs
 * - Conversation stream with clear separation of User messages, Support replies, and Internal Notes
 * - Internal notes composer (strictly isolated from user view)
 * - Complete ticket audit history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSupportSection(
    repository: CineCutRepository,
    currentAdminRole: AdminRole,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val allTickets by repository.supportTickets.collectAsState()
    val ticketMessages by repository.ticketMessages.collectAsState()
    val auditLogs by repository.auditLogs.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: All Tickets, 1: Assigned to Me, 2: Unassigned Queue, 3: Analytics & KPIs
    var selectedTicketId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<SupportTicketStatus?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<SupportTicketCategory?>(null) }
    var selectedPriorityFilter by remember { mutableStateOf<SupportTicketPriority?>(null) }

    val selectedTicket = remember(allTickets, selectedTicketId) {
        allTickets.find { it.ticketId == selectedTicketId }
    }

    val assignedToMeTickets = remember(allTickets, currentUser.uid) {
        allTickets.filter { it.assignedTo == currentUser.uid }
    }

    val unassignedTickets = remember(allTickets) {
        allTickets.filter { it.assignedTo == null && it.status != SupportTicketStatus.CLOSED }
    }

    // Role check: support staff permissions
    val hasSupportAccess = currentAdminRole.hasPermission(AdminPermission.MANAGE_SUPPORT_TICKETS)

    if (!hasSupportAccess) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = CineError, modifier = Modifier.size(48.dp))
                Text("Access Denied", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Your role (${currentAdminRole.name}) does not have permission to access the Support Center.",
                    fontSize = 12.sp,
                    color = CineTextSecondary
                )
            }
        }
        return
    }

    if (selectedTicket != null) {
        // TICKET DETAILS & ACTION INSPECTOR PANEL
        val messages = ticketMessages[selectedTicket!!.ticketId] ?: emptyList()
        val ticketAudits = remember(auditLogs, selectedTicket!!.ticketId) {
            auditLogs.filter { it.target == selectedTicket!!.ticketId }
        }

        AdminTicketInspector(
            ticket = selectedTicket!!,
            messages = messages,
            auditLogs = ticketAudits,
            currentAdmin = currentUser,
            onBack = { selectedTicketId = null },
            onSendMessage = { text, isInternal, attachments ->
                repository.sendSupportMessage(selectedTicket!!.ticketId, text, isInternalNote = isInternal, attachments = attachments)
            },
            onUpdateStatus = { newStatus ->
                repository.updateSupportTicketStatus(selectedTicket!!.ticketId, newStatus)
            },
            onUpdatePriority = { newPriority ->
                repository.updateSupportTicketPriority(selectedTicket!!.ticketId, newPriority)
            },
            onAssignStaff = { uid, name ->
                repository.assignSupportTicket(selectedTicket!!.ticketId, uid, name)
            }
        )
    } else {
        // SUPPORT DASHBOARD & TICKETS MANAGEMENT
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("admin_support_section"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Breadcrumbs & Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(14.dp))
                    Text("Admin Panel", fontSize = 11.sp, color = CineTertiary)
                    Text("›", fontSize = 12.sp, color = CineTextTertiary)
                    Text("Support Center", fontSize = 11.sp, color = CinePrimary, fontWeight = FontWeight.Bold)
                    Text("›", fontSize = 12.sp, color = CineTextTertiary)
                    Text("Tickets", fontSize = 11.sp, color = Color.White)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Customer Support Center",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Manage creator inquiries, triage payment disputes & resolve technical issues",
                            fontSize = 11.sp,
                            color = CineTextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F3D24),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineSuccess)
                    ) {
                        Text(
                            text = "${allTickets.count { it.status == SupportTicketStatus.OPEN }} Open",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminKpiCard(
                    title = "Total",
                    value = "${allTickets.size}",
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Open",
                    value = "${allTickets.count { it.status == SupportTicketStatus.OPEN }}",
                    color = CinePrimary,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "In Progress",
                    value = "${allTickets.count { it.status == SupportTicketStatus.IN_PROGRESS }}",
                    color = CineSecondary,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Unassigned",
                    value = "${unassignedTickets.size}",
                    color = CineWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Navigation Tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = CineSurface,
                contentColor = CinePrimary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("All Tickets (${allTickets.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Assigned to Me (${assignedToMeTickets.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Unassigned (${unassignedTickets.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text("Analytics", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            when (activeTab) {
                0, 1, 2 -> {
                    val baseList = when (activeTab) {
                        1 -> assignedToMeTickets
                        2 -> unassignedTickets
                        else -> allTickets
                    }

                    val filteredTickets = remember(
                        baseList,
                        searchQuery,
                        selectedStatusFilter,
                        selectedCategoryFilter,
                        selectedPriorityFilter
                    ) {
                        baseList.filter { ticket ->
                            val matchesSearch = searchQuery.isBlank() ||
                                    ticket.ticketId.contains(searchQuery, ignoreCase = true) ||
                                    ticket.userDisplayName.contains(searchQuery, ignoreCase = true) ||
                                    ticket.userEmail.contains(searchQuery, ignoreCase = true) ||
                                    (ticket.orderId?.contains(searchQuery, ignoreCase = true) == true) ||
                                    ticket.subject.contains(searchQuery, ignoreCase = true)

                            val matchesStatus = selectedStatusFilter == null || ticket.status == selectedStatusFilter
                            val matchesCategory = selectedCategoryFilter == null || ticket.category == selectedCategoryFilter
                            val matchesPriority = selectedPriorityFilter == null || ticket.priority == selectedPriorityFilter

                            matchesSearch && matchesStatus && matchesCategory && matchesPriority
                        }
                    }

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by Ticket ID (#CC-...), user, email, or order ID...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CineTextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = CineTextSecondary)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_tickets_search_field"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Filters Row
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == null && selectedCategoryFilter == null && selectedPriorityFilter == null,
                                onClick = {
                                    selectedStatusFilter = null
                                    selectedCategoryFilter = null
                                    selectedPriorityFilter = null
                                },
                                label = { Text("All Filters", fontSize = 10.sp) }
                            )
                        }

                        // Status Chips
                        SupportTicketStatus.values().forEach { st ->
                            item {
                                FilterChip(
                                    selected = selectedStatusFilter == st,
                                    onClick = {
                                        selectedStatusFilter = if (selectedStatusFilter == st) null else st
                                    },
                                    label = { Text(st.name, fontSize = 10.sp) }
                                )
                            }
                        }

                        // Priority Chips
                        SupportTicketPriority.values().forEach { prio ->
                            item {
                                FilterChip(
                                    selected = selectedPriorityFilter == prio,
                                    onClick = {
                                        selectedPriorityFilter = if (selectedPriorityFilter == prio) null else prio
                                    },
                                    label = { Text("Prio: ${prio.name}", fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    // Ticket Cards List
                    if (filteredTickets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No tickets found matching the search or filter criteria.", fontSize = 12.sp, color = CineTextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTickets) { ticket ->
                                AdminTicketCard(
                                    ticket = ticket,
                                    onClick = {
                                        selectedTicketId = ticket.ticketId
                                        repository.markTicketReadForAdmin(ticket.ticketId)
                                    }
                                )
                            }
                        }
                    }
                }

                3 -> {
                    // SUPPORT ANALYTICS TAB
                    SupportAnalyticsTab(allTickets = allTickets)
                }
            }
        }
    }
}

/**
 * KPI Summary Card
 */
@Composable
fun AdminKpiCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontSize = 10.sp, color = CineTextTertiary)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

/**
 * Admin Ticket Row Item
 */
@Composable
fun AdminTicketCard(
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
        shape = RoundedCornerShape(10.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (ticket.unreadAdminCount > 0) CinePrimary else CineTimelineRuler
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_ticket_${ticket.ticketId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Text(ticket.ticketId, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                    Surface(shape = RoundedCornerShape(4.dp), color = CineSurfaceHighlight) {
                        Text(ticket.category.title, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (ticket.unreadAdminCount > 0) {
                        Surface(shape = CircleShape, color = CinePrimary) {
                            Text("USER REPLIED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor)
                ) {
                    Text(
                        ticket.status.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                ticket.subject,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "User: ${ticket.userDisplayName} (${ticket.userEmail})",
                    fontSize = 10.sp,
                    color = CineTextSecondary
                )

                if (ticket.orderId != null) {
                    Text("Order: ${ticket.orderId}", fontSize = 10.sp, color = CineSecondary, fontWeight = FontWeight.SemiBold)
                }
            }

            Divider(color = CineTimelineRuler, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (ticket.assignedToName != null) "Assigned: ${ticket.assignedToName}" else "⚠️ Unassigned",
                    fontSize = 10.sp,
                    color = if (ticket.assignedToName != null) CineTertiary else CineWarning
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (ticket.priority) {
                        SupportTicketPriority.URGENT -> CineError.copy(alpha = 0.2f)
                        SupportTicketPriority.HIGH -> CineWarning.copy(alpha = 0.2f)
                        else -> CineSurfaceVariant
                    }
                ) {
                    Text(
                        ticket.priority.name,
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
            }
        }
    }
}

/**
 * Admin Ticket Inspection & Conversation Stream
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketInspector(
    ticket: SupportTicket,
    messages: List<SupportTicketMessage>,
    auditLogs: List<AuditLog>,
    currentAdmin: UserProfile,
    onBack: () -> Unit,
    onSendMessage: (String, Boolean, List<SupportAttachment>) -> Unit,
    onUpdateStatus: (SupportTicketStatus) -> Unit,
    onUpdatePriority: (SupportTicketPriority) -> Unit,
    onAssignStaff: (String?, String?) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isInternalNoteMode by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var selectedAttachments by remember { mutableStateOf<List<SupportAttachment>>(emptyList()) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var activeSubView by remember { mutableIntStateOf(0) } // 0: Chat & Notes, 1: History & Audit

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_ticket_inspector")
    ) {
        // Inspector Header
        Surface(color = CineSurface) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(ticket.ticketId, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(ticket.ticketId)) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = CineTextSecondary, modifier = Modifier.size(12.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Priority Badge (Clickable to change)
                        Surface(
                            onClick = { showPriorityDialog = true },
                            shape = RoundedCornerShape(4.dp),
                            color = when (ticket.priority) {
                                SupportTicketPriority.URGENT -> CineError
                                SupportTicketPriority.HIGH -> CineWarning
                                SupportTicketPriority.MEDIUM -> CineSecondary
                                SupportTicketPriority.LOW -> CineTimelineRuler
                            }
                        ) {
                            Text(
                                "PRIO: ${ticket.priority.name} ▾",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        // Status Badge (Clickable to change)
                        Surface(
                            onClick = { showStatusDialog = true },
                            shape = RoundedCornerShape(4.dp),
                            color = when (ticket.status) {
                                SupportTicketStatus.OPEN -> CinePrimary
                                SupportTicketStatus.IN_PROGRESS -> CineSecondary
                                SupportTicketStatus.WAITING_FOR_USER -> CineTertiary
                                SupportTicketStatus.RESOLVED -> CineSuccess
                                SupportTicketStatus.CLOSED -> CineTextTertiary
                            }
                        ) {
                            Text(
                                "${ticket.status.name} ▾",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Title & Subtitle
                Text(ticket.subject, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                // Creator & Assignment Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Creator: ${ticket.userDisplayName} • ${ticket.userEmail}", fontSize = 10.sp, color = CineTextSecondary)
                        if (ticket.orderId != null) {
                            Text("Payment Order: ${ticket.orderId}", fontSize = 10.sp, color = CineSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Assignee Button
                    OutlinedButton(
                        onClick = { showAssignDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            ticket.assignedToName?.take(16) ?: "Assign Staff",
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Sub View Selector (Chat / History)
        TabRow(
            selectedTabIndex = activeSubView,
            containerColor = CineSurfaceHighlight,
            contentColor = CinePrimary
        ) {
            Tab(
                selected = activeSubView == 0,
                onClick = { activeSubView = 0 },
                text = { Text("Conversation & Notes (${messages.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubView == 1,
                onClick = { activeSubView = 1 },
                text = { Text("History & Audit Trail (${auditLogs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        when (activeSubView) {
            0 -> {
                // CHAT STREAM & NOTE COMPOSER
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        AdminChatMessageItem(msg = msg)
                    }
                }

                // COMPOSER BAR (Supports User Reply or Internal Note)
                Surface(
                    color = if (isInternalNoteMode) Color(0xFF2C240E) else CineSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isInternalNoteMode) CineSecondary else CineTimelineRuler
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Mode Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = !isInternalNoteMode,
                                    onClick = { isInternalNoteMode = false },
                                    label = { Text("Public Reply to User", fontSize = 10.sp) },
                                    leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                )

                                FilterChip(
                                    selected = isInternalNoteMode,
                                    onClick = { isInternalNoteMode = true },
                                    label = { Text("Staff Internal Note", fontSize = 10.sp) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CineSecondary,
                                        selectedLabelColor = Color(0xFF2E1A00)
                                    )
                                )
                            }

                            if (isInternalNoteMode) {
                                Text("🔒 NEVER VISIBLE TO USER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CineSecondary)
                            }
                        }

                        // Attachments Preview
                        if (selectedAttachments.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(selectedAttachments) { att ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CineSurfaceHighlight
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(att.fileName, fontSize = 9.sp, color = Color.White)
                                            IconButton(
                                                onClick = { selectedAttachments = selectedAttachments.filterNot { it.id == att.id } },
                                                modifier = Modifier.size(14.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(10.dp), tint = CineTextSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Input Box & Send
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val att = SupportAttachment(
                                        fileName = "admin_proof_${System.currentTimeMillis().toString().takeLast(4)}.png",
                                        fileUrl = "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=500",
                                        fileType = "image/png",
                                        fileSizeBytes = 192000L
                                    )
                                    selectedAttachments = selectedAttachments + att
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CineSurfaceHighlight)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach File", tint = CineTertiary)
                            }

                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = {
                                    Text(
                                        if (isInternalNoteMode) "Type internal note (only visible to staff)..." else "Write reply to ${ticket.userDisplayName}...",
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_ticket_reply_field"),
                                shape = RoundedCornerShape(20.dp),
                                maxLines = 4
                            )

                            IconButton(
                                onClick = {
                                    if (replyText.isNotBlank() || selectedAttachments.isNotEmpty()) {
                                        onSendMessage(replyText, isInternalNoteMode, selectedAttachments)
                                        replyText = ""
                                        selectedAttachments = emptyList()
                                    }
                                },
                                enabled = replyText.isNotBlank() || selectedAttachments.isNotEmpty(),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (replyText.isNotBlank() || selectedAttachments.isNotEmpty()) (if (isInternalNoteMode) CineSecondary else CinePrimary) else CineSurfaceVariant)
                                    .testTag("admin_send_reply_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (isInternalNoteMode) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // TICKET HISTORY & AUDIT TRAIL
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("COMPLETE TICKET AUDIT LOGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                    }

                    if (auditLogs.isEmpty()) {
                        item {
                            Text("No audit logs recorded for this ticket.", fontSize = 11.sp, color = CineTextSecondary)
                        }
                    }

                    items(auditLogs) { log ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CineSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                                    val df = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
                                    Text(df.format(Date(log.timestamp)), fontSize = 9.sp, color = CineTextTertiary)
                                }
                                Text("Admin / User: ${log.adminUid}", fontSize = 10.sp, color = CineTextSecondary)
                                Text(log.reason, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Assign Staff Dialog
    if (showAssignDialog) {
        val staffList = listOf(
            Pair(currentAdmin.uid, "${currentAdmin.displayName} (Me)"),
            Pair("usr_support_01", "Sarah Jenkins (Support Lead)"),
            Pair("usr_support_02", "David Kim (Creator Ops)"),
            Pair("usr_support_03", "Elena Rostova (Technical Support)"),
            Pair(null, "Unassigned")
        )

        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Assign Support Staff", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    staffList.forEach { (uid, name) ->
                        Surface(
                            onClick = {
                                onAssignStaff(uid, if (uid != null) name else null)
                                showAssignDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (ticket.assignedTo == uid) CineSurfaceHighlight else CineSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = CineTertiary)
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }

    // Status Changer Dialog
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Change Ticket Status", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SupportTicketStatus.values().forEach { st ->
                        Surface(
                            onClick = {
                                onUpdateStatus(st)
                                showStatusDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (ticket.status == st) CineSurfaceHighlight else CineSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = st.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showStatusDialog = false }) { Text("Cancel") } },
            containerColor = CineSurface
        )
    }

    // Priority Changer Dialog
    if (showPriorityDialog) {
        AlertDialog(
            onDismissRequest = { showPriorityDialog = false },
            title = { Text("Change Ticket Priority", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SupportTicketPriority.values().forEach { prio ->
                        Surface(
                            onClick = {
                                onUpdatePriority(prio)
                                showPriorityDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (ticket.priority == prio) CineSurfaceHighlight else CineSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = prio.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPriorityDialog = false }) { Text("Cancel") } },
            containerColor = CineSurface
        )
    }
}

/**
 * Message bubble inside Admin Inspector
 */
@Composable
fun AdminChatMessageItem(msg: SupportTicketMessage) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    if (msg.isInternalNote) {
        // STRICTLY INTERNAL STAFF NOTE
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF33270A),
            border = androidx.compose.foundation.BorderStroke(1.dp, CineSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CineSecondary, modifier = Modifier.size(14.dp))
                        Text(
                            "INTERNAL STAFF NOTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = CineSecondary
                        )
                    }

                    Text(dateFormat.format(Date(msg.createdAt)), fontSize = 9.sp, color = CineSecondary.copy(alpha = 0.8f))
                }

                Text(
                    msg.message,
                    fontSize = 12.sp,
                    color = Color.White
                )

                Text("Written by: ${msg.senderName} (${msg.senderRole})", fontSize = 9.sp, color = CineTextSecondary)
            }
        }
    } else {
        val isUserMessage = msg.senderRole == "USER"

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isUserMessage) Alignment.Start else Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = msg.senderName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUserMessage) CinePrimary else CineTertiary
                )
                Text("•", fontSize = 10.sp, color = CineTextTertiary)
                Text(dateFormat.format(Date(msg.createdAt)), fontSize = 9.sp, color = CineTextTertiary)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isUserMessage) CineSurface else CineSurfaceHighlight,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUserMessage) CineTimelineRuler else CineTertiary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        msg.message,
                        fontSize = 12.sp,
                        color = Color.White
                    )

                    // Attachments preview
                    if (msg.attachments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            msg.attachments.forEach { att ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CineSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(14.dp))
                                        Text(att.fileName, fontSize = 9.sp, color = Color.White)
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
 * Support Analytics & Metrics Visualization
 */
@Composable
fun SupportAnalyticsTab(allTickets: List<SupportTicket>) {
    val total = allTickets.size.coerceAtLeast(1)
    val resolved = allTickets.count { it.status == SupportTicketStatus.RESOLVED || it.status == SupportTicketStatus.CLOSED }
    val open = allTickets.count { it.status == SupportTicketStatus.OPEN }
    val inProgress = allTickets.count { it.status == SupportTicketStatus.IN_PROGRESS }
    val resolutionRate = (resolved.toFloat() / total.toFloat() * 100).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("SUPPORT PERFORMANCE & HEALTH METRICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        // Summary Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resolution Efficiency", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$resolutionRate%", fontSize = 32.sp, fontWeight = FontWeight.Black, color = CineSuccess)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg Response Time: 18m", fontSize = 11.sp, color = CineTertiary, fontWeight = FontWeight.Bold)
                            Text("Avg Resolution: 3h 12m", fontSize = 10.sp, color = CineTextSecondary)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { resolutionRate / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = CineSuccess,
                        trackColor = CineSurfaceVariant,
                    )
                }
            }
        }

        // Category Breakdown
        item {
            Text("TICKETS BY CATEGORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        items(SupportTicketCategory.values()) { cat ->
            val count = allTickets.count { it.category == cat }
            if (count > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CineSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.title, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Surface(shape = RoundedCornerShape(4.dp), color = CineSurfaceHighlight) {
                            Text("$count ticket(s)", fontSize = 10.sp, color = CineTertiary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

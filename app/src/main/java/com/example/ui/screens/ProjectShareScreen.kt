package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectShareScreen(
    repository: CineCutRepository,
    project: Project,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allProjects by repository.projects.collectAsState()
    val currentProject = allProjects.find { it.id == project.id } ?: project

    val collaboratorsMap by repository.collaborators.collectAsState()
    val collaborators = collaboratorsMap[currentProject.id] ?: emptyList()

    val operationsMap by repository.projectOperations.collectAsState()
    val operations = operationsMap[currentProject.id] ?: emptyList()

    val versionsMap by repository.projectVersions.collectAsState()
    val versions = versionsMap[currentProject.id] ?: emptyList()

    val commentsMap by repository.projectComments.collectAsState()
    val comments = commentsMap[currentProject.id] ?: emptyList()

    val friendships by repository.friendships.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Collaborators, 1: Activity & Versions, 2: Comments
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteTargetUid by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf(ProjectRole.EDITOR) }

    var showNewVersionDialog by remember { mutableStateOf(false) }
    var newVersionName by remember { mutableStateOf("") }
    var newVersionSummary by remember { mutableStateOf("") }

    var commentText by remember { mutableStateOf("") }
    var commentTimecode by remember { mutableStateOf("00:04.200") }

    var toastMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentProject.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Share, Roles & Real-Time Activity", fontSize = 11.sp, color = CineTertiary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("project_share_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Visibility Card
            Surface(
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PROJECT VISIBILITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CinePrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                currentProject.visibility,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CinePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Visibility Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("PRIVATE", "FRIENDS", "INVITE_ONLY", "COLLABORATORS", "PUBLIC").forEach { vis ->
                            FilterChip(
                                selected = currentProject.visibility == vis,
                                onClick = {
                                    repository.updateProjectVisibility(currentProject.id, vis)
                                    toastMessage = "Visibility updated to $vis"
                                },
                                label = { Text(vis.take(5), fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = CineSurface,
                contentColor = CinePrimary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Collaborators (${collaborators.size})", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Activity & Versions", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Comments (${comments.size})", fontSize = 11.sp) }
                )
            }

            // Tabs Content
            when (activeTab) {
                0 -> {
                    // Collaborator Management
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TEAM MEMBERS & ROLES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                            Button(
                                onClick = { showInviteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invite Member", fontSize = 11.sp)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(collaborators) { collab ->
                                CollaboratorCard(
                                    collaborator = collab,
                                    isCurrentUserOwner = currentProject.ownerId == currentUser.uid,
                                    onRoleChange = { newRole ->
                                        repository.updateCollaboratorRole(currentProject.id, collab.userId, newRole)
                                        toastMessage = "Updated ${collab.displayName}'s role to ${newRole.label}"
                                    },
                                    onRemove = {
                                        repository.removeCollaborator(currentProject.id, collab.userId)
                                        toastMessage = "Removed ${collab.displayName} from project"
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Activity & Versions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VERSION SNAPSHOTS (${versions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                            Button(
                                onClick = { showNewVersionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CineTertiary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Snapshot", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Version Cards
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(versions) { ver ->
                                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CineSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("v${ver.versionNumber} • ${ver.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(dateFormat.format(Date(ver.timestamp)), fontSize = 10.sp, color = CineTextTertiary)
                                        }
                                        Text(ver.changeSummary, fontSize = 11.sp, color = CineTextSecondary)
                                        Text("Saved by ${ver.authorName}", fontSize = 10.sp, color = CineTertiary)
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("COLLABORATION OPERATIONS LOG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                            }

                            items(operations) { op ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CineSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = CinePrimary, modifier = Modifier.size(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${op.userName} — ${op.operationType}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(op.details, fontSize = 11.sp, color = CineTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Comments & Mentions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("TIMECODE COMMENTS & MENTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(comments) { comment ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CineSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(comment.authorName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = CineSecondary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "@${comment.timecodeMs / 1000}s",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CineSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(comment.text, fontSize = 12.sp, color = CineTextPrimary)
                                    }
                                }
                            }
                        }

                        // Add Comment Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("Comment or @mention...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CineSurface,
                                    unfocusedContainerColor = CineSurface,
                                    focusedBorderColor = CinePrimary,
                                    unfocusedBorderColor = CineTimelineRuler
                                )
                            )
                            Button(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        repository.addProjectComment(
                                            projectId = currentProject.id,
                                            text = commentText,
                                            timecodeMs = 4200L,
                                            mentions = if (commentText.contains("@")) listOf("alex_cinematics") else emptyList()
                                        )
                                        commentText = ""
                                        toastMessage = "Comment posted with timecode link!"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Post")
                            }
                        }
                    }
                }
            }
        }
    }

    // Invite Modal Dialog
    if (showInviteDialog) {
        val eligibleFriends = friendships.map { it.friendProfile }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Collaborator", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a connected friend or enter user UID:", fontSize = 11.sp, color = CineTextSecondary)

                    // Friends Selector
                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(eligibleFriends) { friend ->
                            Surface(
                                onClick = { inviteTargetUid = friend.uid },
                                shape = RoundedCornerShape(6.dp),
                                color = if (inviteTargetUid == friend.uid) CinePrimary.copy(alpha = 0.2f) else CineSurfaceVariant,
                                border = if (inviteTargetUid == friend.uid) androidx.compose.foundation.BorderStroke(1.dp, CinePrimary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(friend.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("@${friend.username}", fontSize = 10.sp, color = CineTextSecondary)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inviteTargetUid,
                        onValueChange = { inviteTargetUid = it },
                        label = { Text("Target User ID") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Assign Collaboration Role:", fontSize = 11.sp, color = CineTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(ProjectRole.EDITOR, ProjectRole.CONTRIBUTOR, ProjectRole.REVIEWER, ProjectRole.ADMIN).forEach { r ->
                            FilterChip(
                                selected = inviteRole == r,
                                onClick = { inviteRole = r },
                                label = { Text(r.label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteTargetUid.isNotBlank()) {
                            val ok = repository.inviteCollaborator(currentProject.id, inviteTargetUid, inviteRole)
                            if (ok) toastMessage = "Collaboration invitation sent!"
                            showInviteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
                ) {
                    Text("Send Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }

    // Save Version Snapshot Dialog
    if (showNewVersionDialog) {
        AlertDialog(
            onDismissRequest = { showNewVersionDialog = false },
            title = { Text("Save Version Snapshot", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newVersionName,
                        onValueChange = { newVersionName = it },
                        label = { Text("Version Label (e.g. Director's Cut)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVersionSummary,
                        onValueChange = { newVersionSummary = it },
                        label = { Text("Change Summary Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVersionName.isNotBlank()) {
                            repository.saveProjectVersion(currentProject.id, newVersionName, newVersionSummary)
                            toastMessage = "Snapshot '$newVersionName' saved!"
                            showNewVersionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineTertiary)
                ) {
                    Text("Save Version", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewVersionDialog = false }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }

    // Toast Notice
    if (toastMessage != null) {
        LaunchedEffect(toastMessage) {
            kotlinx.coroutines.delay(2200)
            toastMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CineSurfaceHighlight,
                border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary)
            ) {
                Text(toastMessage!!, fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
fun CollaboratorCard(
    collaborator: Collaborator,
    isCurrentUserOwner: Boolean,
    onRoleChange: (ProjectRole) -> Unit,
    onRemove: () -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CineSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Text(collaborator.displayName.take(1), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(collaborator.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("@${collaborator.username}", fontSize = 11.sp, color = CineTextSecondary)
            }

            Box {
                Surface(
                    onClick = { if (isCurrentUserOwner && collaborator.role != ProjectRole.OWNER) expandedDropdown = true },
                    shape = RoundedCornerShape(6.dp),
                    color = when (collaborator.role) {
                        ProjectRole.OWNER -> CinePrimary.copy(alpha = 0.2f)
                        ProjectRole.ADMIN -> CineTertiary.copy(alpha = 0.2f)
                        else -> CineSurfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(collaborator.role.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        if (isCurrentUserOwner && collaborator.role != ProjectRole.OWNER) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.background(CineSurface)
                ) {
                    listOf(ProjectRole.ADMIN, ProjectRole.EDITOR, ProjectRole.CONTRIBUTOR, ProjectRole.REVIEWER, ProjectRole.VIEWER).forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r.label, color = Color.White) },
                            onClick = {
                                onRoleChange(r)
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            if (isCurrentUserOwner && collaborator.role != ProjectRole.OWNER) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = CineError)
                }
            }
        }
    }
}

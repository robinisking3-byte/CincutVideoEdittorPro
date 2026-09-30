package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AdminPermission
import com.example.core.model.AdminRole
import com.example.core.model.ModerationReport
import com.example.core.model.ReportStatus
import com.example.core.model.hasPermission
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsSection(
    reports: List<ModerationReport>,
    currentAdminRole: AdminRole,
    onUpdateReport: (reportId: String, newStatus: ReportStatus, resolution: String?, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatusFilter by remember { mutableStateOf<ReportStatus?>(null) } // null = All
    var selectedReportForDetail by remember { mutableStateOf<ModerationReport?>(null) }
    var resolutionText by remember { mutableStateOf("") }
    var actionDialogType by remember { mutableStateOf<ReportStatus?>(null) }

    val filteredReports = remember(reports, selectedStatusFilter) {
        if (selectedStatusFilter == null) reports else reports.filter { it.status == selectedStatusFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null },
                    label = { Text("All (${reports.size})", fontSize = 11.sp) }
                )
            }
            ReportStatus.values().forEach { status ->
                item {
                    val count = reports.count { it.status == status }
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text("${status.name} ($count)", fontSize = 11.sp) }
                    )
                }
            }
        }

        Text(
            text = "MODERATION REPORTS QUEUE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextTertiary
        )

        // Reports List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredReports) { report ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CineSurface)
                        .border(
                            1.dp,
                            when (report.status) {
                                ReportStatus.PENDING -> CineWarning.copy(alpha = 0.5f)
                                ReportStatus.REVIEWING -> CineTertiary.copy(alpha = 0.5f)
                                ReportStatus.RESOLVED -> CineSuccess.copy(alpha = 0.3f)
                                ReportStatus.ESCALATED -> CineError.copy(alpha = 0.5f)
                                ReportStatus.REJECTED -> CineTextTertiary
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CineSurfaceHighlight
                                ) {
                                    Text(
                                        report.targetType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CinePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text("ID: ${report.targetId}", fontSize = 11.sp, color = CineTextSecondary)
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (report.status) {
                                    ReportStatus.PENDING -> CineWarning.copy(alpha = 0.2f)
                                    ReportStatus.REVIEWING -> CineTertiary.copy(alpha = 0.2f)
                                    ReportStatus.RESOLVED -> CineSuccess.copy(alpha = 0.2f)
                                    ReportStatus.ESCALATED -> CineError.copy(alpha = 0.2f)
                                    ReportStatus.REJECTED -> CineSurfaceVariant
                                }
                            ) {
                                Text(
                                    text = report.status.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (report.status) {
                                        ReportStatus.PENDING -> CineWarning
                                        ReportStatus.REVIEWING -> CineTertiary
                                        ReportStatus.RESOLVED -> CineSuccess
                                        ReportStatus.ESCALATED -> CineError
                                        ReportStatus.REJECTED -> CineTextSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Reason: ${report.reason}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (report.description.isNotBlank()) {
                            Text(
                                text = report.description,
                                fontSize = 11.sp,
                                color = CineTextSecondary
                            )
                        }

                        if (report.targetSnippet.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CineSurfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Target Snippet: \"${report.targetSnippet}\"",
                                    fontSize = 11.sp,
                                    color = CineTextPrimary
                                )
                            }
                        }

                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        Text(
                            text = "Reported by ${report.reporterName} • ${dateFormat.format(Date(report.timestamp))}",
                            fontSize = 10.sp,
                            color = CineTextTertiary
                        )

                        if (report.resolution != null) {
                            Text(
                                text = "Resolution: ${report.resolution}",
                                fontSize = 11.sp,
                                color = CineSuccess
                            )
                        }

                        Divider(color = CineTimelineRuler, thickness = 0.5.dp)

                        // Quick Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { selectedReportForDetail = report }
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Inspect Full Report", fontSize = 11.sp)
                            }

                            if (currentAdminRole.hasPermission(AdminPermission.MODERATE_CONTENT)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (report.status != ReportStatus.RESOLVED) {
                                        Button(
                                            onClick = {
                                                selectedReportForDetail = report
                                                actionDialogType = ReportStatus.RESOLVED
                                                resolutionText = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CineSuccess),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Resolve", fontSize = 10.sp)
                                        }
                                    }
                                    if (report.status != ReportStatus.REJECTED && report.status != ReportStatus.RESOLVED) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedReportForDetail = report
                                                actionDialogType = ReportStatus.REJECTED
                                                resolutionText = "Dismissed: No policy violation found"
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Reject", fontSize = 10.sp)
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

    // Detail Dialog / Action Dialog
    if (selectedReportForDetail != null && actionDialogType != null) {
        val rep = selectedReportForDetail!!
        val targetStatus = actionDialogType!!
        AlertDialog(
            onDismissRequest = { actionDialogType = null },
            title = {
                Text(
                    text = "${targetStatus.name} Report ${rep.id.take(8)}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Target: ${rep.targetType} (${rep.targetId})\nReason: ${rep.reason}",
                        fontSize = 12.sp,
                        color = CineTextSecondary
                    )
                    OutlinedTextField(
                        value = resolutionText,
                        onValueChange = { resolutionText = it },
                        label = { Text("Resolution Notes / Sanction Applied") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateReport(
                            rep.id,
                            targetStatus,
                            resolutionText.ifBlank { "Status updated to ${targetStatus.name}" },
                            resolutionText
                        )
                        actionDialogType = null
                        selectedReportForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (targetStatus == ReportStatus.RESOLVED) CineSuccess else CinePrimary
                    )
                ) {
                    Text("Apply Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { actionDialogType = null }) {
                    Text("Cancel")
                }
            },
            containerColor = CineSurface
        )
    }
}

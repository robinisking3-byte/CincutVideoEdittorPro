package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.MembershipTier
import com.example.ui.theme.*

@Composable
fun MembershipBadge(
    tier: MembershipTier,
    modifier: Modifier = Modifier
) {
    val tierGradient = when (tier) {
        MembershipTier.FREE -> listOf(Color(0xFF475569), Color(0xFF334155))
        MembershipTier.BRONZE -> listOf(Color(0xFFCD7F32), Color(0xFF8B4513))
        MembershipTier.SILVER -> listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
        MembershipTier.GOLD -> listOf(Color(0xFFFFD700), Color(0xFFF59E0B))
        MembershipTier.DIAMOND -> listOf(Color(0xFF00E5FF), Color(0xFF0077B6))
        MembershipTier.VIP -> listOf(Color(0xFFFF007F), Color(0xFF7928CA))
        MembershipTier.FOUNDER -> listOf(Color(0xFFFF4500), Color(0xFFFF8C00))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(tierGradient.map { it.copy(alpha = 0.25f) }))
            .border(1.dp, Brush.horizontalGradient(tierGradient), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = when (tier) {
                MembershipTier.FREE -> Icons.Default.Person
                MembershipTier.BRONZE, MembershipTier.SILVER, MembershipTier.GOLD -> Icons.Default.WorkspacePremium
                MembershipTier.DIAMOND -> Icons.Default.Diamond
                MembershipTier.VIP, MembershipTier.FOUNDER -> Icons.Default.Stars
            },
            contentDescription = tier.title,
            tint = Color(tier.badgeColor),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = tier.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
fun CineCoinBadge(
    balance: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(16.dp),
        color = CineSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, CineSecondary.copy(alpha = 0.5f)),
        modifier = modifier.testTag("cine_coin_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFFFE066), CineSecondary))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¢",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF332000)
                )
            }
            Text(
                text = "%,d".format(balance),
                color = CineSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Verified Creator",
        tint = CineTertiary,
        modifier = modifier.size(16.dp)
    )
}

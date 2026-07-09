package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UsageQuotaSnapshot
import com.example.ui.theme.*

@Composable
fun UsageQuotaCard(
    usageQuota: UsageQuotaSnapshot,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !usageQuota.isPro) { onUpgradeClick() }
            .testTag("usage_quota_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealCard),
        border = BorderStroke(
            1.dp,
            if (usageQuota.isPro) DreamTeal.copy(alpha = 0.5f) else DreamGold.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(DreamPurple, DreamGold)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (usageQuota.isPro) Icons.Default.Verified else Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (usageQuota.isPro) "Dream Weaver Pro" else "Free plan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (usageQuota.isPro) DreamTeal else DreamGold
                )
                Text(
                    text = if (usageQuota.isPro) {
                        "Unlimited AI dream analyses"
                    } else {
                        "${usageQuota.remaining} of ${usageQuota.monthlyLimit} AI analyses left this month"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
            if (!usageQuota.isPro) {
                Text(
                    text = "Upgrade",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DreamTeal
                )
            }
        }
    }
}


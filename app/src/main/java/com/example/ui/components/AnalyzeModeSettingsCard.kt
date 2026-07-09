package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag

@Composable
fun AnalyzeModeSettingsCard(
    analyzeWithAi: Boolean,
    onAnalyzeWithAiChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealCard),
        border = BorderStroke(1.dp, EtherealCardBorder)
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
                        Brush.linearGradient(colors = listOf(DreamGold, DreamPurple)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (analyzeWithAi) "Analyze with AI now" else "Analyze later (recommended)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (analyzeWithAi) DreamTeal else DreamGold
                )
                Text(
                    text = if (analyzeWithAi) {
                        "Dreams use Gemini immediately when saved."
                    } else {
                        "Dreams save instantly. Open them and tap Analyze Dream when ready."
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
            Switch(
                checked = analyzeWithAi,
                onCheckedChange = onAnalyzeWithAiChanged,
                modifier = Modifier.testTag("dashboard_analyze_with_ai_switch")
            )
        }
    }
}


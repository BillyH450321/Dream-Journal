package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DreamGold

@Composable
fun ProFeatureBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(DreamGold.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, DreamGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "PRO",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = DreamGold,
            letterSpacing = 0.5.sp
        )
    }
}


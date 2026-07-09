package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Dream
import com.example.ui.theme.*

@Composable
fun DreamStatsCard(dreams: List<Dream>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealCard),
        border = BorderStroke(1.dp, EtherealCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dreams.size.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DreamPurple
                )
                Text(
                    text = "Dreams Saved",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(EtherealCardBorder)
            )

            val dreamsWithImages = dreams.count { it.surrealImagePath != null }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dreamsWithImages.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DreamTeal
                )
                Text(
                    text = "Surreal Art",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(EtherealCardBorder)
            )

            // Calculate active nights: span of days
            val activeNights = if (dreams.isEmpty()) 1 else {
                val dates = dreams.map { it.timestamp }
                val max = dates.maxOrNull() ?: 0L
                val min = dates.minOrNull() ?: 0L
                val diffDays = ((max - min) / (1000 * 60 * 60 * 24)).toInt() + 1
                diffDays
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeNights.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DreamGold
                )
                Text(
                    text = "Active Days",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}


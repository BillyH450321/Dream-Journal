package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DreamTeal
import com.example.ui.theme.NebulaLavender
import com.example.ui.theme.TextSecondary

@Composable
fun InterpretationSection(title: String, body: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lens,
                contentDescription = null,
                tint = when {
                    title.contains("Theme", ignoreCase = true) || title.contains("Landscape", ignoreCase = true) -> DreamPurple
                    title.contains("Archetype", ignoreCase = true) -> DreamTeal
                    title.contains("Symbol", ignoreCase = true) || title.contains("Integration", ignoreCase = true) || title.contains("Path", ignoreCase = true) -> DreamGold
                    else -> NebulaLavender
                },
                modifier = Modifier.size(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    title.contains("Theme", ignoreCase = true) || title.contains("Landscape", ignoreCase = true) -> DreamPurple
                    title.contains("Archetype", ignoreCase = true) -> DreamTeal
                    title.contains("Symbol", ignoreCase = true) || title.contains("Integration", ignoreCase = true) || title.contains("Path", ignoreCase = true) -> DreamGold
                    else -> NebulaLavender
                }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body.trim(),
            fontSize = 14.sp,
            color = NebulaLavender.copy(alpha = 0.9f),
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)

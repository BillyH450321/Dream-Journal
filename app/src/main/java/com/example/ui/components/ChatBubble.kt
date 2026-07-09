package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.ui.theme.*

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(DeepVioletAccent, CircleShape)
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = DreamGold,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Card(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) DreamPurple else EtherealCard
            ),
            border = if (message.isUser) null else BorderStroke(1.dp, EtherealCardBorder)
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                color = if (message.isUser) CosmicBackground else NebulaLavender,
                lineHeight = 20.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(DreamTeal, CircleShape)
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CosmicBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Renders structured markdown interpretations decodably and beautifully in Jetpack Compose
 */

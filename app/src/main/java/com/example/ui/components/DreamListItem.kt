package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Dream
import com.example.ui.theme.*
import com.example.ui.util.displayTitle
import com.example.ui.util.needsAnalysis
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DreamListItem(
    dream: Dream,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(dream.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        sdf.format(Date(dream.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("dream_item_card_${dream.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealCard),
        border = BorderStroke(1.dp, EtherealCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Generated image or a beautiful placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepVioletAccent)
            ) {
                if (dream.surrealImagePath != null) {
                    AsyncImage(
                        model = File(dream.surrealImagePath),
                        contentDescription = "Surreal theme artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Synthesizing",
                            tint = DreamPurple.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Center: Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = DreamTeal,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dream.displayTitle(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NebulaLavender,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dream.rawText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                if (dream.emotionalTheme != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lens,
                            contentDescription = null,
                            tint = DreamGold,
                            modifier = Modifier.size(6.dp)
                        )
                        Text(
                            text = dream.emotionalTheme.substringBefore("\n").trim().removePrefix("#").trim(),
                            fontSize = 12.sp,
                            color = DreamGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (dream.needsAnalysis()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (dream.analysisStatus == "failed") "Analysis failed — tap to retry" else "Awaiting AI analysis",
                        fontSize = 11.sp,
                        color = DreamGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (dream.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dream.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(DreamTeal.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, DreamTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 10.sp,
                                    color = DreamTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Right: Delete Button
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.testTag("delete_dream_button_${dream.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete entry",
                    tint = TextSecondary
                )
            }
        }
    }
}

// --- Recorder Screen ---


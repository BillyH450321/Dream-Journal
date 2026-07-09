package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.ui.DreamJournalViewModel
import com.example.ui.components.ArchetypeChip
import com.example.ui.components.ChatBubble
import com.example.ui.components.ProFeatureBadge
import com.example.ui.components.StyledMarkdownCard
import com.example.ui.navigation.Routes
import com.example.ui.theme.*
import com.example.ui.util.displayTitle
import com.example.ui.util.formatAudioTime
import com.example.ui.util.isAnalysisInProgress
import com.example.ui.util.needsAnalysis
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal fun Modifier.fillAsMaxSize() = this.fillMaxSize()

@Composable
fun DetailScreen(
    viewModel: DreamJournalViewModel,
    navController: NavHostController
) {
    val dream by viewModel.selectedDream.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isSendingChat by viewModel.isSendingChatMessage.collectAsStateWithLifecycle()
    val audioPlayback by viewModel.audioPlaybackState.collectAsStateWithLifecycle()
    val analyzingDreamId by viewModel.analyzingDreamId.collectAsStateWithLifecycle()
    val usageQuota by viewModel.usageQuota.collectAsStateWithLifecycle()

    var chatInputText by remember { mutableStateOf("") }
    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose { viewModel.stopAudioPlayback() }
    }

    val formattedDate = remember(dream?.timestamp) {
        val stamp = dream?.timestamp ?: 0L
        val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy · hh:mm a", Locale.getDefault())
        sdf.format(Date(stamp))
    }

    val lazyListState = rememberLazyListState()

    // Scroll chat to the last message when a new one arrives
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatHistory.size + 1) // accounts for other static sections
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sophisticated App Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = EtherealCard),
                border = BorderStroke(1.dp, EtherealCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Go Back",
                            tint = NebulaLavender
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.sweepGradient(
                                        colors = listOf(DreamPurple, DreamTeal, DreamPurple)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Dream Decoder",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NebulaLavender
                            )
                            val formattedTime = remember(dream?.timestamp) {
                                val stamp = dream?.timestamp ?: System.currentTimeMillis()
                                val sdf = SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault())
                                sdf.format(Date(stamp))
                            }
                            Text(
                                text = formattedTime.uppercase(),
                                fontSize = 10.sp,
                                color = DreamTeal,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    IconButton(onClick = {
                        dream?.id?.let {
                            viewModel.deleteDream(it)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextSecondary
                        )
                    }
                }
            }

            if (dream == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DreamPurple)
                }
            } else {
                val currentDream = dream!!
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillAsMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // 1. Date Header Space
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 2. Surreal Art Image Card - Aspect Ratio 4:3
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .border(1.dp, EtherealCardBorder, RoundedCornerShape(32.dp))
                                .background(EtherealCard)
                        ) {
                            if (currentDream.artworkStatus == "complete" && currentDream.surrealImagePath != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = File(currentDream.surrealImagePath),
                                        contentDescription = "Generated Dream Surreal Art",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Overlay Text on bottom of Image Card: bg-gradient-to-t from-black/80 to-transparent
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                )
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Column {
                                            if (currentDream.artworkFallbackUsed) {
                                                Text(
                                                    text = "abstract interpretation",
                                                    fontSize = 10.sp,
                                                    color = DreamTeal,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(bottom = 4.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "\"${currentDream.rawText}\"",
                                                fontSize = 14.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                fontFamily = FontFamily.Serif,
                                                color = Color(0xFFE2E2E6),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                    // Regenerate Button Overlay
                                    IconButton(
                                        onClick = {
                                            if (usageQuota.isPro) {
                                                viewModel.regenerateDreamArtwork(currentDream.id)
                                            } else {
                                                navController.navigate(Routes.PAYWALL)
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (usageQuota.isPro) Icons.Default.Refresh else Icons.Default.Lock,
                                            contentDescription = if (usageQuota.isPro) "Regenerate Artwork" else "Pro feature",
                                            tint = if (usageQuota.isPro) Color.White else DreamGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                // Beautiful gradient placeholder for Surrealist Image
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF1A1C1E),
                                                    Color(0xFF2D1B3E),
                                                    Color(0xFF111827)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Custom radial glow overlay using Canvas or custom drawBehind
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .drawBehind {
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(DreamTeal.copy(alpha = 0.25f), Color.Transparent),
                                                        radius = size.width * 0.7f
                                                    ),
                                                    center = this.center
                                                )
                                            }
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (currentDream.artworkStatus == "failed") {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = "Generation Failed",
                                                tint = Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Artwork generation failed",
                                                fontSize = 13.sp,
                                                color = TextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    if (usageQuota.isPro) {
                                                        viewModel.regenerateDreamArtwork(currentDream.id)
                                                    } else {
                                                        navController.navigate(Routes.PAYWALL)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DreamPurple)
                                            ) {
                                                Text(
                                                    if (usageQuota.isPro) "Try Again" else "Upgrade to Retry",
                                                    color = Color.White
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Synthesizing",
                                                tint = Color.White.copy(alpha = 0.2f),
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Consulting subconscious art...",
                                                fontSize = 13.sp,
                                                color = TextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2.5 Analyze Later CTA
                    if (currentDream.needsAnalysis() || currentDream.isAnalysisInProgress(analyzingDreamId)) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .testTag("analyze_dream_card"),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = EtherealCard),
                                border = BorderStroke(1.dp, DreamGold.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Ready for AI Analysis",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DreamGold
                                    )
                                    Text(
                                        text = if (currentDream.analysisStatus == "failed")
                                            "Last analysis failed (rate limit or server busy). Wait a minute, then try again."
                                        else
                                            "This dream was saved without using the API. Tap below when you're ready for transcription, interpretation, tags, and artwork.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                    Button(
                                        onClick = { viewModel.analyzeDream(currentDream.id) },
                                        enabled = !currentDream.isAnalysisInProgress(analyzingDreamId),
                                        colors = ButtonDefaults.buttonColors(containerColor = DreamPurple),
                                        modifier = Modifier.testTag("analyze_dream_button")
                                    ) {
                                        if (currentDream.isAnalysisInProgress(analyzingDreamId)) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Analyzing...", color = Color.White)
                                        } else {
                                            Text(
                                                if (currentDream.analysisStatus == "failed") "Retry Analysis" else "Analyze Dream",
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Dream Title Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = EtherealCard),
                            border = BorderStroke(1.dp, EtherealCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Dream Title",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DreamGold
                                    )
                                    if (!isEditingTitle) {
                                        IconButton(
                                            onClick = {
                                                editedTitle = currentDream.title ?: currentDream.displayTitle()
                                                isEditingTitle = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("edit_title_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit title",
                                                tint = DreamTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isEditingTitle) {
                                    OutlinedTextField(
                                        value = editedTitle,
                                        onValueChange = { editedTitle = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("edit_title_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = NebulaLavender,
                                            unfocusedTextColor = NebulaLavender,
                                            focusedBorderColor = DreamTeal,
                                            unfocusedBorderColor = EtherealCardBorder,
                                            focusedContainerColor = CosmicBackground,
                                            unfocusedContainerColor = CosmicBackground
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        TextButton(
                                            onClick = { isEditingTitle = false },
                                            modifier = Modifier.testTag("cancel_title_button")
                                        ) {
                                            Text("Cancel", color = TextSecondary)
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.updateDreamTitle(currentDream.id, editedTitle)
                                                isEditingTitle = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DreamPurple),
                                            modifier = Modifier.testTag("save_title_button")
                                        ) {
                                            Text("Save", color = Color.White)
                                        }
                                    }
                                } else if (currentDream.needsAnalysis()) {
                                    Text(
                                        text = "Title will be generated after analysis",
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = TextSecondary
                                    )
                                } else if (currentDream.title.isNullOrBlank()) {
                                    Text(
                                        text = "Generating title…",
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = TextSecondary
                                    )
                                } else {
                                    Text(
                                        text = currentDream.title,
                                        fontSize = 22.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 28.sp,
                                        color = NebulaLavender
                                    )
                                }
                            }
                        }
                    }

                    // 3.5. Voice Recording Playback
                    if (!currentDream.audioPath.isNullOrBlank()) {
                        item {
                            val audioPath = currentDream.audioPath!!
                            val isCurrentDreamAudio = audioPlayback.dreamId == currentDream.id
                            val isPlaying = isCurrentDreamAudio && audioPlayback.isPlaying
                            val currentPosition = if (isCurrentDreamAudio) audioPlayback.currentPositionMs else 0
                            val duration = if (isCurrentDreamAudio) audioPlayback.durationMs else 0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .testTag("audio_playback_card"),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = EtherealCard),
                                border = BorderStroke(1.dp, EtherealCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "Original Voice Recording",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DreamTeal
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                viewModel.toggleAudioPlayback(currentDream.id, audioPath)
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(DreamPurple, CircleShape)
                                                .testTag("audio_play_pause_button")
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = Color.White
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Slider(
                                                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                                onValueChange = { progress ->
                                                    if (duration > 0) {
                                                        viewModel.seekAudioPlayback((progress * duration).roundToInt())
                                                    }
                                                },
                                                modifier = Modifier.testTag("audio_seek_slider"),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = DreamTeal,
                                                    activeTrackColor = DreamTeal,
                                                    inactiveTrackColor = EtherealCardBorder
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = formatAudioTime(currentPosition),
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = formatAudioTime(duration),
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    val audioError = audioPlayback.error
                                    if (isCurrentDreamAudio && audioError != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = audioError,
                                            fontSize = 12.sp,
                                            color = Color(0xFFFF8A80)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Transcript Text Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = EtherealCard),
                            border = BorderStroke(1.dp, EtherealCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Subconscious Recall",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DreamPurple
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"${currentDream.rawText}\"",
                                    fontSize = 15.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    lineHeight = 22.sp,
                                    color = NebulaLavender
                                )
                            }
                        }
                    }

                    // 3.5. Custom Tag Management Section
                    item {
                        var newTagText by remember { mutableStateOf("") }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = EtherealCard),
                            border = BorderStroke(1.dp, EtherealCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Dream Tags",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DreamTeal
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val tagsList = remember(currentDream.tags) {
                                    if (currentDream.tags.isBlank()) emptyList() else currentDream.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                }
                                
                                if (tagsList.isEmpty()) {
                                    Text(
                                        text = "No custom tags added yet. Use the input below to add tags like 'flying', 'family', or 'anxiety'.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        tagsList.forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(DreamTeal.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                    .border(1.dp, DreamTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                    .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                                                    .testTag("dream_tag_chip_$tag"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = tag,
                                                        fontSize = 12.sp,
                                                        color = NebulaLavender,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.removeTagFromDream(currentDream.id, tag)
                                                        },
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .testTag("delete_tag_button_$tag")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remove tag $tag",
                                                            tint = TextSecondary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newTagText,
                                        onValueChange = { newTagText = it },
                                        placeholder = { Text("Add custom tag...", color = TextSecondary, fontSize = 13.sp) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("add_tag_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = NebulaLavender,
                                            unfocusedTextColor = NebulaLavender,
                                            focusedBorderColor = DreamTeal,
                                            unfocusedBorderColor = EtherealCardBorder,
                                            focusedContainerColor = CosmicBackground,
                                            unfocusedContainerColor = CosmicBackground
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            if (newTagText.isNotBlank()) {
                                                viewModel.addTagToDream(currentDream.id, newTagText)
                                                newTagText = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(DreamTeal, RoundedCornerShape(12.dp))
                                            .testTag("add_tag_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add tag",
                                            tint = CosmicBackground
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Structured Archetypal Interpretation
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Psychological Analysis",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = NebulaLavender,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Dynamic Archetype Chips matching design spec perfectly
                            val tags = remember(currentDream.emotionalTheme, currentDream.rawText) {
                                val list = mutableListOf<String>()
                                if (!currentDream.emotionalTheme.isNullOrBlank()) {
                                    list.add(currentDream.emotionalTheme.substringBefore("\n").trim().removePrefix("#").trim())
                                }
                                val text = currentDream.rawText.lowercase()
                                if (text.contains("shadow") || text.contains("dark") || text.contains("run") || text.contains("chase")) {
                                    list.add("The Shadow")
                                } else if (text.contains("water") || text.contains("sea") || text.contains("river") || text.contains("ocean")) {
                                    list.add("Unconscious")
                                } else if (text.contains("fly") || text.contains("sky") || text.contains("wind")) {
                                    list.add("Spirit")
                                } else {
                                    list.add("Anima")
                                }
                                list.add("Threshold")
                                list.distinct().take(3)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tags.forEachIndexed { index, tag ->
                                    val chipColor = when (index) {
                                        0 -> DreamTeal
                                        1 -> DreamPurple
                                        else -> DreamGold
                                    }
                                    ArchetypeChip(text = tag, color = chipColor)
                                }
                            }

                            val interpretationText = currentDream.structuredInterpretation
                            if (currentDream.needsAnalysis()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = EtherealCard.copy(alpha = 0.6f)),
                                    border = BorderStroke(1.dp, EtherealCardBorder)
                                ) {
                                    Text(
                                        text = "Psychological analysis will appear here after you tap Analyze Dream.",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            } else if (interpretationText != null) {
                                StyledMarkdownCard(interpretationText)
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = EtherealCard),
                                    border = BorderStroke(1.dp, EtherealCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = DreamPurple)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Consulting Archetypal Symbols...",
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Follow-Up Chat Divider
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(EtherealCardBorder))
                                Text(
                                    text = " Ask Symbol Interpreter ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DreamGold
                                )
                                if (!usageQuota.isPro) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ProFeatureBadge()
                                }
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(EtherealCardBorder))
                            }
                        }
                    }

                    // 6. Interactive Chat Bubble History
                    if (currentDream.needsAnalysis()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = EtherealCard.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, EtherealCardBorder)
                            ) {
                                Text(
                                    text = "Symbol chat unlocks after you analyze this dream.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else if (chatHistory.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = EtherealCard.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, EtherealCardBorder)
                            ) {
                                Text(
                                    text = "Ask questions about specific elements, symbols, or feelings in your dream. E.g. \"What does the silver water signify?\" or \"Why was my shadow fleeing?\"",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(chatHistory, key = { it.id }) { message ->
                            ChatBubble(message)
                        }
                    }

                    // 7. AI Thinking Loader
                    if (isSendingChat) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DeepVioletAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = DreamTeal,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = EtherealCard),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, EtherealCardBorder)
                                ) {
                                    Text(
                                        text = "Analyzing symbols...",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!currentDream.needsAnalysis()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicBackground)
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (!usageQuota.isPro) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate(Routes.PAYWALL) }
                                    .testTag("chat_pro_lock_card"),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = EtherealCard),
                                border = BorderStroke(1.dp, DreamGold.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = DreamGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Symbol chat is a Pro feature",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NebulaLavender
                                        )
                                        Text(
                                            text = "Tap to upgrade and ask about symbols in your dreams.",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        text = "Upgrade",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DreamTeal
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(EtherealCard, RoundedCornerShape(32.dp))
                                    .border(1.dp, EtherealCardBorder, RoundedCornerShape(32.dp))
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chatInputText,
                                    onValueChange = { chatInputText = it },
                                    placeholder = { Text("Ask about a symbol...", color = TextSecondary, fontSize = 14.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = NebulaLavender,
                                        unfocusedTextColor = NebulaLavender,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = CosmicBackground,
                                        unfocusedContainerColor = CosmicBackground
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (chatInputText.isNotBlank()) {
                                            viewModel.sendChatMessage(chatInputText)
                                            chatInputText = ""
                                            keyboardController?.hide()
                                        }
                                    })
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .shadow(6.dp, CircleShape, clip = false)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(DreamPurple, DreamTeal)
                                            ),
                                            CircleShape
                                        )
                                        .clickable(enabled = chatInputText.isNotBlank()) {
                                            viewModel.sendChatMessage(chatInputText)
                                            chatInputText = ""
                                            keyboardController?.hide()
                                        }
                                        .testTag("chat_send_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.Send,
                                        contentDescription = "Send Message",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.ui.DreamJournalViewModel
import com.example.ui.RecordingState
import com.example.ui.theme.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily

@Composable
fun RecorderScreen(
    viewModel: DreamJournalViewModel,
    navController: NavHostController
) {
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle()
    var isManualTextMode by remember { mutableStateOf(false) }
    var typedDreamText by remember { mutableStateOf("") }
    val analyzeWithAiDefault by viewModel.analyzeWithAiDefault.collectAsStateWithLifecycle()
    var analyzeWithAiNow by remember(analyzeWithAiDefault) { mutableStateOf(analyzeWithAiDefault) }

    LaunchedEffect(analyzeWithAiDefault) {
        analyzeWithAiNow = analyzeWithAiDefault
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = {
                viewModel.cancelVoiceRecording()
                navController.popBackStack()
            },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = "Back",
                tint = NebulaLavender
            )
        }

        // Core processing layout
        if (recordingState is RecordingState.Processing) {
            val stage = (recordingState as RecordingState.Processing).stage
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = DreamPurple,
                    strokeWidth = 6.dp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Aetheria Synapse",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = NebulaLavender
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stage,
                    fontSize = 14.sp,
                    color = DreamTeal,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        } else if (recordingState is RecordingState.Error) {
            val errorMsg = (recordingState as RecordingState.Error).message
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Aether Connection Failed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NebulaLavender
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMsg,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.resetRecordingState() },
                    colors = ButtonDefaults.buttonColors(containerColor = DreamPurple)
                ) {
                    Text("Retry Recording")
                }
            }
        } else {
            // Recording / Input Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isManualTextMode) "Write Your Dream" else "Speak Into the Void",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = NebulaLavender
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (analyzeWithAiNow) {
                            if (isManualTextMode)
                                "Type your dream — AI will analyze, tag, and generate artwork."
                            else
                                "Record your dream — AI will transcribe and analyze when you stop."
                        } else {
                            "Save now, analyze later. No AI calls until you tap Analyze Dream."
                        },
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EtherealCard),
                        border = BorderStroke(1.dp, EtherealCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (analyzeWithAiNow) "Analyze with AI now" else "Analyze later",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (analyzeWithAiNow) DreamTeal else DreamGold
                                )
                                Text(
                                    text = if (analyzeWithAiNow) "Uses Gemini API immediately" else "Recommended — avoids rate limits",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = analyzeWithAiNow,
                                onCheckedChange = {
                                    analyzeWithAiNow = it
                                    viewModel.setAnalyzeWithAiDefault(it)
                                },
                                modifier = Modifier.testTag("analyze_with_ai_switch")
                            )
                        }
                    }
                }

                if (!isManualTextMode) {
                    // Voice recording interface
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Pulse circle animations
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(180.dp)
                        ) {
                            if (recordingState is RecordingState.Recording) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.radialGradient(
                                                listOf(
                                                    DreamPurple.copy(alpha = pulseAlpha),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .scale(pulseScale)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .shadow(8.dp, CircleShape)
                                    .background(
                                        if (recordingState is RecordingState.Recording) Color.Red else DreamPurple,
                                        CircleShape
                                    )
                                    .clickable {
                                        if (recordingState is RecordingState.Recording) {
                                            viewModel.stopVoiceRecording(analyzeNow = analyzeWithAiNow)
                                        } else {
                                            viewModel.startVoiceRecording()
                                        }
                                    }
                                    .testTag("record_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (recordingState is RecordingState.Recording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Mic Trigger",
                                    tint = CosmicBackground,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Live Recording Timer
                        val minutes = String.format("%02d", recordingDuration / 60)
                        val seconds = String.format("%02d", recordingDuration % 60)
                        Text(
                            text = "$minutes:$seconds",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (recordingState is RecordingState.Recording) Color.Red else NebulaLavender
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (recordingState is RecordingState.Recording) "Recording dream audio..." else "Tap mic to start",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    // Manual typing interface
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = typedDreamText,
                            onValueChange = { typedDreamText = it },
                            label = { Text("Write your dream here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("typed_dream_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = NebulaLavender,
                                unfocusedTextColor = NebulaLavender,
                                focusedBorderColor = DreamPurple,
                                unfocusedBorderColor = EtherealCardBorder,
                                focusedContainerColor = EtherealCard,
                                unfocusedContainerColor = EtherealCard
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.processManualDreamText(
                                    typedDreamText,
                                    analyzeNow = analyzeWithAiNow
                                )
                            },
                            enabled = typedDreamText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (analyzeWithAiNow) DreamPurple else DreamGold,
                                disabledContainerColor = EtherealCardBorder
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                if (analyzeWithAiNow) "Save & Analyze Now" else "Save for Later",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicBackground
                            )
                        }
                    }
                }

                // Toggle interface
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Recording",
                        fontSize = 13.sp,
                        color = if (!isManualTextMode) DreamTeal else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isManualTextMode,
                        onCheckedChange = { isManualTextMode = it },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Text(
                        text = "Type Dream",
                        fontSize = 13.sp,
                        color = if (isManualTextMode) DreamPurple else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- Detail Screen ---


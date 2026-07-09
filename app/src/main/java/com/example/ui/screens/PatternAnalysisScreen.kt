package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.ui.DreamJournalViewModel
import com.example.ui.PatternAnalysisState
import com.example.ui.components.ProFeatureBadge
import com.example.ui.components.StyledMarkdownCard
import com.example.ui.navigation.Routes
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternAnalysisScreen(
    viewModel: DreamJournalViewModel,
    navController: NavHostController
) {
    val analysisState by viewModel.patternAnalysisState.collectAsStateWithLifecycle()
    val dreams by viewModel.allDreams.collectAsStateWithLifecycle()
    val usageQuota by viewModel.usageQuota.collectAsStateWithLifecycle()

    // Load cached report on launch
    LaunchedEffect(Unit) {
        viewModel.loadPatternAnalysis()
    }

    // Trigger analysis if Idle and we have recorded dreams (Pro only)
    LaunchedEffect(analysisState, dreams, usageQuota.isPro) {
        if (usageQuota.isPro && analysisState is PatternAnalysisState.Idle && dreams.isNotEmpty()) {
            viewModel.generatePatternAnalysis()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Background radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(DeepVioletAccent.copy(alpha = 0.5f), Color.Transparent),
                        radius = size.width * 1.5f
                    ),
                    center = this.center
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant App Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = EtherealCard),
                border = BorderStroke(1.dp, EtherealCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("pattern_analysis_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = NebulaLavender
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Pattern Analysis",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NebulaLavender
                        )
                        val subtitleText = when (val state = analysisState) {
                            is PatternAnalysisState.Success -> "Synthesized across ${state.dreamCountAnalyzed} dreams"
                            PatternAnalysisState.Loading -> "Decoding subconscious trends..."
                            else -> "Subconscious insights"
                        }
                        Text(
                            text = subtitleText.uppercase(),
                            fontSize = 10.sp,
                            color = DreamTeal,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (dreams.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("empty_dreams_analysis_card"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = EtherealCard),
                            border = BorderStroke(1.dp, EtherealCardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Canvas(modifier = Modifier.size(80.dp)) {
                                    drawCircle(
                                        color = DreamGold.copy(alpha = 0.7f),
                                        radius = 35f
                                    )
                                    drawCircle(
                                        color = CosmicBackground,
                                        radius = 35f,
                                        center = this.center.copy(x = this.center.x - 20f, y = this.center.y - 8f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Subconscious is Quiet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NebulaLavender
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pattern analysis requires dreams in your journal. Please voice-record or type some dreams on the dashboard first, and the AI will analyze recurring themes!",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else if (!usageQuota.isPro) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("pattern_analysis_pro_lock_card"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = EtherealCard),
                            border = BorderStroke(1.dp, DreamGold.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = DreamGold,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Pattern Insights",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NebulaLavender
                                    )
                                    ProFeatureBadge()
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cross-dream pattern analysis is a Pro feature. Upgrade to discover recurring symbols, emotional threads, and archetypes across your journal.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { navController.navigate(Routes.PAYWALL) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DreamPurple),
                                    modifier = Modifier.testTag("pattern_analysis_upgrade_button")
                                ) {
                                    Text("Upgrade to Pro", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    when (val state = analysisState) {
                        is PatternAnalysisState.Idle, is PatternAnalysisState.Loading -> {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("loading_analysis_card"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = EtherealCard),
                                    border = BorderStroke(1.dp, EtherealCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            color = DreamPurple,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "Weaving Dream Insights...",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NebulaLavender
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Our Jungian AI analyst is reading your dream journal to synthesize recurring symbols, deep emotional threads, and major archetypes. This can take up to a minute.",
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                        is PatternAnalysisState.Success -> {
                            item {
                                StyledMarkdownCard(markdownText = state.report)
                            }

                            item {
                                Button(
                                    onClick = { viewModel.generatePatternAnalysis() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("refresh_pattern_analysis_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepVioletAccent,
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, DreamTeal.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Icon",
                                            tint = DreamTeal
                                        )
                                        Text(
                                            text = "Refresh Subconscious Analysis",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NebulaLavender
                                        )
                                    }
                                }
                            }
                        }
                        is PatternAnalysisState.Error -> {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("error_analysis_card"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = EtherealCard),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error Icon",
                                            tint = Color.Red,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Analysis Disrupted",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NebulaLavender
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = state.message,
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.generatePatternAnalysis() },
                                            colors = ButtonDefaults.buttonColors(containerColor = DreamPurple)
                                        ) {
                                            Text("Retry Analysis")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper helper

package com.example.ui.screens

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.ui.DreamJournalViewModel
import com.example.ui.components.AnalyzeModeSettingsCard
import com.example.ui.components.DreamListItem
import com.example.ui.components.DreamStatsCard
import com.example.ui.components.ProFeatureBadge
import com.example.ui.components.UsageQuotaCard
import com.example.ui.navigation.Routes
import com.example.ui.theme.*
import com.example.ui.util.filterDreams
import android.Manifest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.shadow

@Composable
fun DashboardScreen(
    viewModel: DreamJournalViewModel,
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    val dreams by viewModel.allDreams.collectAsStateWithLifecycle()
    val analyzeWithAiDefault by viewModel.analyzeWithAiDefault.collectAsStateWithLifecycle()
    val usageQuota by viewModel.usageQuota.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    val filteredDreams = remember(dreams, searchQuery) { filterDreams(dreams, searchQuery) }

    // Request permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate(Routes.RECORDER)
        } else {
            // Advise user text mode can be used as fallback
            android.widget.Toast.makeText(context, "Microphone permission is required for voice recording, but you can still type your dreams!", android.widget.Toast.LENGTH_LONG).show()
            navController.navigate(Routes.RECORDER)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Background radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(DeepVioletAccent.copy(alpha = 0.45f), Color.Transparent),
                        radius = size.width * 1.2f
                    ),
                    center = this.center
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Bar matching header styling exactly from Sophisticated Dark spec
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = EtherealCard),
                border = BorderStroke(1.dp, EtherealCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = NebulaLavender
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(DreamPurple, DreamTeal)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Dream Weaver",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NebulaLavender
                            )
                            val currentDate = remember {
                                val sdf = SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault())
                                sdf.format(Date())
                            }
                            Text(
                                text = currentDate.uppercase(),
                                fontSize = 11.sp,
                                color = DreamTeal,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Account & Plan",
                            tint = DreamGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                UsageQuotaCard(
                    usageQuota = usageQuota,
                    onUpgradeClick = { navController.navigate(Routes.PAYWALL) }
                )
                Spacer(modifier = Modifier.height(16.dp))

                AnalyzeModeSettingsCard(
                    analyzeWithAi = analyzeWithAiDefault,
                    onAnalyzeWithAiChanged = viewModel::setAnalyzeWithAiDefault
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Subconscious Statistics Dashboard
                if (dreams.isNotEmpty()) {
                    DreamStatsCard(dreams = dreams)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (usageQuota.isPro) {
                                    navController.navigate(Routes.PATTERN_ANALYSIS)
                                } else {
                                    navController.navigate(Routes.PAYWALL)
                                }
                            }
                            .testTag("pattern_analysis_cta_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = EtherealCard),
                        border = BorderStroke(1.dp, EtherealCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(DreamPurple, DreamTeal)
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Subconscious Pattern Insights",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NebulaLavender
                                    )
                                    if (!usageQuota.isPro) {
                                        ProFeatureBadge()
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (usageQuota.isPro) {
                                        "Analyze recurring symbols, emotional threads, and archetypes across your journal."
                                    } else {
                                        "Unlock Pro to discover recurring symbols and archetypes across your journal."
                                    },
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                            Icon(
                                imageVector = if (usageQuota.isPro) {
                                    Icons.AutoMirrored.Filled.ArrowForward
                                } else {
                                    Icons.Default.Lock
                                },
                                contentDescription = if (usageQuota.isPro) "View Patterns" else "Pro feature",
                                tint = if (usageQuota.isPro) DreamTeal else DreamGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text(
                    text = "Dream Chronicles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Styled modern search bar
                if (dreams.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by keywords or tags...", color = TextSecondary, fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("dream_search_input"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = DreamTeal
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NebulaLavender,
                            unfocusedTextColor = NebulaLavender,
                            focusedBorderColor = DreamPurple,
                            unfocusedBorderColor = EtherealCardBorder,
                            focusedContainerColor = EtherealCard,
                            unfocusedContainerColor = EtherealCard
                        ),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )
                }

                if (filteredDreams.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            if (dreams.isEmpty()) {
                                // Empty State with canvas crescent moon
                                Canvas(modifier = Modifier.size(100.dp)) {
                                    // Crescent moon path
                                    drawCircle(
                                        color = DreamGold.copy(alpha = 0.85f),
                                        radius = 45f
                                    )
                                    drawCircle(
                                        color = CosmicBackground, // subtractive circle to make crescent
                                        radius = 45f,
                                        center = this.center.copy(x = this.center.x - 25f, y = this.center.y - 10f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Silence in the Ether",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NebulaLavender,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your subconscious is quiet. Tap the glowing button below to speak or write your first dream immediately upon awakening.",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            } else {
                                // Search Empty State
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "No search results",
                                    tint = DreamTeal.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Dream Matches",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NebulaLavender,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No dream entries match your query. Try searching for other keywords, emotions, or custom tags.",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredDreams, key = { it.id }) { dream ->
                            DreamListItem(
                                dream = dream,
                                onClick = {
                                    navController.navigate(Routes.detail(dream.id))
                                },
                                onDelete = {
                                    viewModel.deleteDream(dream.id)
                                }
                            )
                        }
                    }
                }
        }
    }

        // Immersive glowing Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(16.dp, CircleShape, clip = false)
                    .background(
                        Brush.linearGradient(
                            listOf(DreamPurple, DreamTeal)
                        ),
                        CircleShape
                    )
                    .clickable {
                        // Request record permission or navigate
                        val recordPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (recordPermission == PackageManager.PERMISSION_GRANTED) {
                            navController.navigate(Routes.RECORDER)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    .testTag("add_dream_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Capture Dream",
                    tint = CosmicBackground,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}


package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.data.ChatMessage
import com.example.data.Dream
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private fun Dream.displayTitle(): String =
    title?.takeIf { it.isNotBlank() && it != "Voice Dream" }
        ?: rawText.lineSequence().firstOrNull()?.take(60)?.trim().orEmpty().ifBlank { "Untitled Dream" }

private fun Dream.needsAnalysis(): Boolean =
    analysisStatus == "deferred" || analysisStatus == "failed"

private fun Dream.isAnalysisInProgress(analyzingDreamId: Long?): Boolean =
    analysisStatus == "pending" || analyzingDreamId == id

private fun formatAudioTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// --- Navigation Routes ---
object Routes {
    const val DASHBOARD = "dashboard"
    const val RECORDER = "recorder"
    const val DETAIL = "detail/{dreamId}"
    const val PATTERN_ANALYSIS = "pattern_analysis"
    const val SETTINGS = "settings"
    fun detail(id: Long) = "detail/$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamJournalApp(
    viewModel: DreamJournalViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val dreams by viewModel.allDreams.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Redirect to detail screen immediately on successful transcription/generation
    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.Success) {
            val dreamId = (recordingState as RecordingState.Success).dreamId
            navController.navigate(Routes.detail(dreamId)) {
                popUpTo(Routes.DASHBOARD) { saveState = true }
                launchSingleTop = true
            }
            viewModel.resetRecordingState()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CosmicBackground,
                drawerContentColor = NebulaLavender,
                modifier = Modifier.width(320.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Past Dreams",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DreamTeal,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                HorizontalDivider(color = EtherealCardBorder, modifier = Modifier.padding(bottom = 8.dp))

                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("open_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = DreamGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("API Settings", color = DreamGold, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = EtherealCardBorder, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                
                if (dreams.isEmpty()) {
                    Text(
                        text = "Your dream journal is empty.",
                        color = TextSecondary,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(dreams, key = { it.id }) { dream ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { drawerState.close() }
                                        navController.navigate(Routes.detail(dream.id)) {
                                            launchSingleTop = true
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (dream.surrealImagePath != null) {
                                    AsyncImage(
                                        model = File(dream.surrealImagePath),
                                        contentDescription = "Dream artwork",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(DreamPurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .border(1.dp, DreamPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = DreamTeal, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val dateStr = remember(dream.timestamp) {
                                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dream.timestamp))
                                    }
                                    Text(
                                        text = dream.displayTitle(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NebulaLavender,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateStr,
                                        fontSize = 11.sp,
                                        color = DreamTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dream.rawText,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(color = EtherealCardBorder, modifier = Modifier.padding(horizontal = 24.dp))
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        viewModel = viewModel,
                        navController = navController,
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        }
                    )
                }
                composable(Routes.RECORDER) {
                    RecorderScreen(
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("dreamId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val dreamId = backStackEntry.arguments?.getLong("dreamId") ?: 0L
                    // Select dream in ViewModel
                    LaunchedEffect(dreamId) {
                        viewModel.selectDream(dreamId)
                    }
                    DetailScreen(
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                composable(Routes.PATTERN_ANALYSIS) {
                    PatternAnalysisScreen(
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: DreamJournalViewModel,
    navController: NavHostController
) {
    val storedApiKey by viewModel.storedApiKey.collectAsStateWithLifecycle()
    val apiTestState by viewModel.apiTestState.collectAsStateWithLifecycle()
    var apiKeyInput by remember(storedApiKey) { mutableStateOf(storedApiKey) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = NebulaLavender)
                    }
                    Column {
                        Text("API Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NebulaLavender)
                        Text(
                            "GEMINI API KEY",
                            fontSize = 10.sp,
                            color = DreamTeal,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = EtherealCard),
                    border = BorderStroke(1.dp, EtherealCardBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Gemini API Key",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DreamGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Paste your key from aistudio.google.com/apikey. Saved on this device only — no rebuild needed.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input"),
                            placeholder = { Text("AIza...", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = NebulaLavender,
                                unfocusedTextColor = NebulaLavender,
                                focusedBorderColor = DreamTeal,
                                unfocusedBorderColor = EtherealCardBorder,
                                focusedContainerColor = CosmicBackground,
                                unfocusedContainerColor = CosmicBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.saveApiKey(apiKeyInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = DreamPurple),
                                modifier = Modifier.testTag("save_api_key_button")
                            ) {
                                Text("Save Key", color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { viewModel.testApiConnection() },
                                modifier = Modifier.testTag("test_api_key_button")
                            ) {
                                Text("Test Connection", color = DreamTeal)
                            }
                        }
                    }
                }

                when (val state = apiTestState) {
                    ApiTestState.Idle -> Unit
                    ApiTestState.Loading -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = EtherealCard)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = DreamPurple, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Text("Testing Gemini connection...", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                    is ApiTestState.Result -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = EtherealCard),
                            border = BorderStroke(
                                1.dp,
                                if (state.success) DreamTeal.copy(alpha = 0.5f) else Color(0xFFFF8A80).copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = state.message,
                                modifier = Modifier.padding(20.dp),
                                color = if (state.success) DreamTeal else Color(0xFFFF8A80),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EtherealCard.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, EtherealCardBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("If you still get HTTP 503:", fontWeight = FontWeight.Bold, color = NebulaLavender, fontSize = 13.sp)
                        Text("• Wait 1–2 minutes and tap Test Connection again", color = TextSecondary, fontSize = 12.sp)
                        Text("• Create a new key at aistudio.google.com/apikey", color = TextSecondary, fontSize = 12.sp)
                        Text("• Ensure the Generative Language API is enabled for your Google project", color = TextSecondary, fontSize = 12.sp)
                        Text("• Try on Wi‑Fi instead of mobile data", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- Dashboard Screen ---

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

@Composable
fun DashboardScreen(
    viewModel: DreamJournalViewModel,
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    val dreams by viewModel.allDreams.collectAsStateWithLifecycle()
    val analyzeWithAiDefault by viewModel.analyzeWithAiDefault.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    val filteredDreams = remember(dreams, searchQuery) {
        if (searchQuery.isBlank()) {
            dreams
        } else {
            val query = searchQuery.lowercase().trim()
            dreams.filter { dream ->
                dream.rawText.lowercase().contains(query) ||
                        dream.title?.lowercase()?.contains(query) == true ||
                        dream.tags.lowercase().split(",").any { tag -> tag.trim().contains(query) }
            }
        }
    }

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
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2D3035), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Cosmic",
                            tint = DreamGold,
                            modifier = Modifier.size(20.dp)
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
                                navController.navigate(Routes.PATTERN_ANALYSIS)
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
                                Text(
                                    text = "Subconscious Pattern Insights",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NebulaLavender
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Analyze recurring symbols, emotional threads, and archetypes across your journal.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Patterns",
                                tint = DreamTeal,
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
                                        onClick = { viewModel.regenerateDreamArtwork(currentDream.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Regenerate Artwork",
                                            tint = Color.White,
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
                                                onClick = { viewModel.regenerateDreamArtwork(currentDream.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = DreamPurple)
                                            ) {
                                                Text("Try Again", color = Color.White)
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

                                    if (isCurrentDreamAudio && audioPlayback.error != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = audioPlayback.error,
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

@Composable
fun ArchetypeChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

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
@Composable
fun StyledMarkdownCard(markdownText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealCard),
        border = BorderStroke(1.dp, EtherealCardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Very basic but visually superb markdown parsing
            val lines = markdownText.lines()
            var currentSectionTitle = ""
            var currentSectionBody = StringBuilder()

            for (line in lines) {
                if (line.startsWith("###")) {
                    // Flush previous section
                    if (currentSectionTitle.isNotBlank()) {
                        InterpretationSection(title = currentSectionTitle, body = currentSectionBody.toString())
                        Spacer(modifier = Modifier.height(16.dp))
                        currentSectionBody = StringBuilder()
                    }
                    currentSectionTitle = line.removePrefix("###").trim()
                } else if (line.startsWith("##") || line.startsWith("#")) {
                    // Flush
                    if (currentSectionTitle.isNotBlank()) {
                        InterpretationSection(title = currentSectionTitle, body = currentSectionBody.toString())
                        Spacer(modifier = Modifier.height(16.dp))
                        currentSectionBody = StringBuilder()
                    }
                    currentSectionTitle = line.removePrefix("##").removePrefix("#").trim()
                } else {
                    currentSectionBody.append(line).append("\n")
                }
            }

            // Flush final section
            if (currentSectionTitle.isNotBlank()) {
                InterpretationSection(title = currentSectionTitle, body = currentSectionBody.toString())
            } else if (markdownText.isNotBlank()) {
                // If parsing fails or doesn't use standard headers, output raw
                Text(
                    text = markdownText,
                    fontSize = 14.sp,
                    color = NebulaLavender,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

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
@Composable
fun PatternAnalysisScreen(
    viewModel: DreamJournalViewModel,
    navController: NavHostController
) {
    val analysisState by viewModel.patternAnalysisState.collectAsStateWithLifecycle()
    val dreams by viewModel.allDreams.collectAsStateWithLifecycle()

    // Load cached report on launch
    LaunchedEffect(Unit) {
        viewModel.loadPatternAnalysis()
    }

    // Trigger analysis if Idle and we have recorded dreams
    LaunchedEffect(analysisState, dreams) {
        if (analysisState is PatternAnalysisState.Idle && dreams.isNotEmpty()) {
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
private fun Modifier.fillAsMaxSize() = this.fillMaxSize()

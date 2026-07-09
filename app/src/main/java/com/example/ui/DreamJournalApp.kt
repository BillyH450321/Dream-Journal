package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.ui.navigation.Routes
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.PaywallScreen
import com.example.ui.screens.PatternAnalysisScreen
import com.example.ui.screens.RecorderScreen
import com.example.ui.screens.AccountPlanScreen
import com.example.ui.theme.*
import com.example.ui.util.displayTitle
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    val paywallRequested by viewModel.paywallRequested.collectAsStateWithLifecycle()

    LaunchedEffect(paywallRequested) {
        if (paywallRequested) {
            viewModel.clearPaywallRequest()
            navController.navigate(Routes.PAYWALL) { launchSingleTop = true }
        }
    }

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
                    Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = DreamGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Account & Plan", color = DreamGold, fontWeight = FontWeight.Bold)
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
                    AccountPlanScreen(
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                composable(Routes.PAYWALL) {
                    PaywallScreen(
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}


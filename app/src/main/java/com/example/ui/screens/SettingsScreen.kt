package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.BuildConfig
import com.example.ui.ApiTestState
import com.example.ui.DreamJournalViewModel
import com.example.ui.components.ProFeatureBadge
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: DreamJournalViewModel,
    navController: NavHostController
) {
    val storedApiKey by viewModel.storedApiKey.collectAsStateWithLifecycle()
    val apiTestState by viewModel.apiTestState.collectAsStateWithLifecycle()
    val usageQuota by viewModel.usageQuota.collectAsStateWithLifecycle()
    val isProDevOverride by viewModel.isProDevOverride.collectAsStateWithLifecycle()
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

                if (BuildConfig.DEBUG) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = EtherealCard),
                        border = BorderStroke(1.dp, DreamGold.copy(alpha = 0.3f))
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
                                    imageVector = Icons.Default.Verified,
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
                                        text = "Unlock Pro (debug only)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DreamGold
                                    )
                                    ProFeatureBadge()
                                }
                                Text(
                                    text = "Local dev override. Release builds use Google Play subscriptions only.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = isProDevOverride,
                                onCheckedChange = viewModel::setProDevOverrideForTesting,
                                modifier = Modifier.testTag("pro_testing_switch")
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Dashboard Screen ---


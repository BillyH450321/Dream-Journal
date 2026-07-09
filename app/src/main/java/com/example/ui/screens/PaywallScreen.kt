package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.ui.DreamJournalViewModel
import com.example.ui.theme.*

@Composable
fun PaywallScreen(
    viewModel: DreamJournalViewModel,
    navController: NavHostController
) {
    val usageQuota by viewModel.usageQuota.collectAsStateWithLifecycle()
    val billingState by viewModel.billingUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(usageQuota.isPro) {
        if (usageQuota.isPro) {
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = NebulaLavender)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(DreamPurple, DreamTeal, DreamGold)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Dream Weaver Pro",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = NebulaLavender,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Understand your subconscious without limits",
                fontSize = 14.sp,
                color = DreamTeal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            val benefits = listOf(
                "Unlimited AI dream analyses",
                "Surreal artwork for every dream",
                "Symbol interpreter chat",
                "Cross-dream pattern insights",
                "Regenerate artwork anytime"
            )
            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DreamTeal, modifier = Modifier.size(20.dp))
                    Text(benefit, fontSize = 14.sp, color = NebulaLavender)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EtherealCard),
                border = BorderStroke(1.dp, EtherealCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = billingState.monthlyPrice?.let { "$it / month" } ?: "\$4.99 / month",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DreamGold
                    )
                    Button(
                        onClick = {
                            activity?.let { viewModel.purchaseProMonthly(it) }
                        },
                        enabled = !billingState.purchaseInProgress && activity != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paywall_monthly_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DreamPurple)
                    ) {
                        if (billingState.purchaseInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Subscribe Monthly", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = EtherealCardBorder)

                    Text(
                        text = billingState.yearlyPrice?.let { "$it / year" } ?: "\$39.99 / year (save 33%)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DreamTeal
                    )
                    OutlinedButton(
                        onClick = {
                            activity?.let { viewModel.purchaseProYearly(it) }
                        },
                        enabled = !billingState.purchaseInProgress && activity != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paywall_yearly_button"),
                        border = BorderStroke(1.dp, DreamTeal)
                    ) {
                        Text("Subscribe Yearly", color = DreamTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }

            billingState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    fontSize = 12.sp,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { viewModel.restorePurchases() },
                modifier = Modifier.testTag("paywall_restore_button")
            ) {
                Text("Restore purchases", color = DreamTeal)
            }

            if (!usageQuota.isPro) {
                Text(
                    text = "You have ${usageQuota.remaining} free analyses left this month. Saving dreams is always free.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { navController.popBackStack() }) {
                Text("Maybe later", color = TextSecondary)
            }
        }
    }
}
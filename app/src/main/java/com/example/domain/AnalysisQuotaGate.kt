package com.example.domain

import com.example.data.UsageQuotaStore
import kotlinx.coroutines.flow.first

class AnalysisQuotaGate(
    private val usageQuotaStore: UsageQuotaStore,
    private val onPaywallRequested: () -> Unit
) {
    suspend fun reserveAnalysisSlot(): Boolean {
        if (usageQuotaStore.reserveAnalysisSlot()) {
            return true
        }
        onPaywallRequested()
        return false
    }

    suspend fun releaseAnalysisSlot() {
        usageQuotaStore.releaseAnalysisSlot()
    }

    suspend fun requirePro(): Boolean {
        if (usageQuotaStore.usageSnapshot.first().isPro) {
            return true
        }
        onPaywallRequested()
        return false
    }
}
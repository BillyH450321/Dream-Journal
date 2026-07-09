package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.usageQuotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "dream_journal_usage")

data class UsageQuotaSnapshot(
    val isPro: Boolean,
    val usedThisMonth: Int,
    val monthlyLimit: Int = UsageQuotaStore.FREE_MONTHLY_ANALYSIS_LIMIT
) {
    val remaining: Int
        get() = if (isPro) Int.MAX_VALUE else (monthlyLimit - usedThisMonth).coerceAtLeast(0)

    val canAnalyze: Boolean
        get() = isPro || usedThisMonth < monthlyLimit
}

class UsageQuotaStore(
    private val context: Context,
    private val monthKeyProvider: () -> String = {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }
) {
    companion object {
        const val FREE_MONTHLY_ANALYSIS_LIMIT = 3
    }

    private val isProPreference = booleanPreferencesKey("is_pro_user")
    private val analysisCountPreference = intPreferencesKey("analysis_count_month")
    private val analysisMonthPreference = stringPreferencesKey("analysis_month_key")

    val usageSnapshot: Flow<UsageQuotaSnapshot> = context.usageQuotaDataStore.data.map { prefs ->
        val currentMonth = currentMonthKey()
        val storedMonth = prefs[analysisMonthPreference]
        val used = if (storedMonth == currentMonth) {
            prefs[analysisCountPreference] ?: 0
        } else {
            0
        }
        val isPro = prefs[isProPreference] ?: false

        UsageQuotaSnapshot(
            isPro = isPro,
            usedThisMonth = used,
            monthlyLimit = FREE_MONTHLY_ANALYSIS_LIMIT
        )
    }

    suspend fun reserveAnalysisSlot(): Boolean {
        var allowed = false
        context.usageQuotaDataStore.edit { prefs ->
            val isPro = prefs[isProPreference] ?: false
            if (isPro) {
                allowed = true
                return@edit
            }

            val currentMonth = currentMonthKey()
            val storedMonth = prefs[analysisMonthPreference]
            val used = if (storedMonth == currentMonth) {
                prefs[analysisCountPreference] ?: 0
            } else {
                0
            }

            if (used >= FREE_MONTHLY_ANALYSIS_LIMIT) {
                allowed = false
                return@edit
            }

            prefs[analysisMonthPreference] = currentMonth
            prefs[analysisCountPreference] = used + 1
            allowed = true
        }
        return allowed
    }

    suspend fun releaseAnalysisSlot() {
        context.usageQuotaDataStore.edit { prefs ->
            val isPro = prefs[isProPreference] ?: false
            if (isPro) return@edit

            val currentMonth = currentMonthKey()
            val storedMonth = prefs[analysisMonthPreference]
            if (storedMonth != currentMonth) return@edit

            val used = prefs[analysisCountPreference] ?: 0
            if (used > 0) {
                prefs[analysisCountPreference] = used - 1
            }
        }
    }

    suspend fun setProUser(enabled: Boolean) {
        context.usageQuotaDataStore.edit { prefs ->
            prefs[isProPreference] = enabled
        }
    }

    private fun currentMonthKey(): String = monthKeyProvider()
}
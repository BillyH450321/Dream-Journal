package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UsageQuotaStoreTest {

    private lateinit var context: Context
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `reserveAnalysisSlot allows three free analyses per month`() = runBlocking {
        val store = UsageQuotaStore(context, monthKeyProvider = { "2026-07-reserve" })

        assertTrue(store.reserveAnalysisSlot())
        assertTrue(store.reserveAnalysisSlot())
        assertTrue(store.reserveAnalysisSlot())
        assertFalse(store.reserveAnalysisSlot())

        val snapshot = store.usageSnapshot.first()
        assertEquals(3, snapshot.usedThisMonth)
        assertEquals(0, snapshot.remaining)
        assertFalse(snapshot.canAnalyze)
    }

    @Test
    fun `releaseAnalysisSlot refunds a failed analysis`() = runBlocking {
        val store = UsageQuotaStore(context, monthKeyProvider = { "2026-08-refund" })

        assertTrue(store.reserveAnalysisSlot())
        assertTrue(store.reserveAnalysisSlot())
        store.releaseAnalysisSlot()

        val snapshot = store.usageSnapshot.first()
        assertEquals(1, snapshot.usedThisMonth)
        assertEquals(2, snapshot.remaining)
        assertTrue(store.reserveAnalysisSlot())
    }

    @Test
    fun `pro users bypass monthly limits`() = runBlocking {
        val store = UsageQuotaStore(context, monthKeyProvider = { "2026-09-pro" })
        store.setProUser(true)

        repeat(5) {
            assertTrue(store.reserveAnalysisSlot())
        }

        val snapshot = store.usageSnapshot.first()
        assertTrue(snapshot.isPro)
        assertTrue(snapshot.canAnalyze)
    }
}
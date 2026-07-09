package com.example.billing

/**
 * Play Console subscription product IDs.
 * Create matching subscriptions before testing purchases:
 * - dream_weaver_pro_monthly
 * - dream_weaver_pro_yearly
 */
object BillingProducts {
    const val PRO_MONTHLY = "dream_weaver_pro_monthly"
    const val PRO_YEARLY = "dream_weaver_pro_yearly"

    val ALL_SUBSCRIPTIONS = listOf(PRO_MONTHLY, PRO_YEARLY)
}
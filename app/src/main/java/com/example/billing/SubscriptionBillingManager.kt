package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.example.data.UsageQuotaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionBillingManager(
    private val context: Context,
    private val usageQuotaStore: UsageQuotaStore,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener {

    private val tag = "SubscriptionBilling"

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private var monthlyDetails: ProductDetails? = null
    private var yearlyDetails: ProductDetails? = null

    fun start() {
        if (billingClient.isReady) {
            scope.launch {
                queryProducts()
                refreshPurchases()
            }
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProducts()
                        refreshPurchases()
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isReady = false,
                        errorMessage = billingErrorMessage(result, "setup")
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _uiState.value = _uiState.value.copy(isReady = false)
            }
        })
    }

    fun purchaseMonthly(activity: Activity) {
        launchPurchase(activity, monthlyDetails, BillingProducts.PRO_MONTHLY)
    }

    fun purchaseYearly(activity: Activity) {
        launchPurchase(activity, yearlyDetails, BillingProducts.PRO_YEARLY)
    }

    fun restorePurchases() {
        scope.launch { refreshPurchases() }
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _uiState.value = _uiState.value.copy(purchaseInProgress = false)

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    handlePurchases(purchases.orEmpty())
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = billingErrorMessage(result, "purchase")
                )
            }
        }
    }

    private fun launchPurchase(activity: Activity, details: ProductDetails?, productLabel: String) {
        if (details == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Subscription not available yet. Add \"$productLabel\" in Play Console."
            )
            return
        }

        val offerToken = details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
        if (offerToken == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No subscription offer found for $productLabel."
            )
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        _uiState.value = _uiState.value.copy(purchaseInProgress = true, errorMessage = null)
        val launchResult = billingClient.launchBillingFlow(activity, params)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _uiState.value = _uiState.value.copy(
                purchaseInProgress = false,
                errorMessage = billingErrorMessage(launchResult, "launch")
            )
        }
    }

    private suspend fun queryProducts() {
        if (!billingClient.isReady) return

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                BillingProducts.ALL_SUBSCRIPTIONS.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _uiState.value = _uiState.value.copy(
                errorMessage = billingErrorMessage(result.billingResult, "product query")
            )
            return
        }

        val products = result.productDetailsList.orEmpty()
        monthlyDetails = products.find { it.productId == BillingProducts.PRO_MONTHLY }
        yearlyDetails = products.find { it.productId == BillingProducts.PRO_YEARLY }

        _uiState.value = _uiState.value.copy(
            isReady = true,
            monthlyPrice = monthlyDetails?.formattedPrice(),
            yearlyPrice = yearlyDetails?.formattedPrice(),
            errorMessage = null
        )
    }

    private suspend fun refreshPurchases() {
        if (!billingClient.isReady) return

        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _uiState.value = _uiState.value.copy(
                errorMessage = billingErrorMessage(result.billingResult, "restore")
            )
            return
        }

        handlePurchases(result.purchasesList)
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        var hasActivePro = false

        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!purchase.products.any { it in BillingProducts.ALL_SUBSCRIPTIONS }) continue

            if (!purchase.isAcknowledged) {
                val ackResult = billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                )
                if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(tag, "Failed to acknowledge purchase: ${ackResult.debugMessage}")
                    continue
                }
            }
            hasActivePro = true
        }

        usageQuotaStore.setProFromBilling(hasActivePro)
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun ProductDetails.formattedPrice(): String? =
        subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice

    private fun billingErrorMessage(result: BillingResult, action: String): String {
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                "Google Play Billing is not available on this device."
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                "Billing service is temporarily unavailable. Try again shortly."
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                "This subscription is not configured in Play Console yet."
            BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                "Billing configuration error. Check Play Console product IDs."
            else -> "Billing $action failed (${result.debugMessage})"
        }
    }
}
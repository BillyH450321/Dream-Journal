package com.example.billing

data class BillingUiState(
    val isReady: Boolean = false,
    val monthlyPrice: String? = null,
    val yearlyPrice: String? = null,
    val purchaseInProgress: Boolean = false,
    val errorMessage: String? = null
)
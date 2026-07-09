package com.example.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val RECORDER = "recorder"
    const val DETAIL = "detail/{dreamId}"
    const val PATTERN_ANALYSIS = "pattern_analysis"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"
    fun detail(id: Long) = "detail/$id"
}
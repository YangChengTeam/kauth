package com.yangfeisg.kauth.feature.scan

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SCAN_ROUTE = "scan"

fun NavGraphBuilder.scanScreen(onBack: () -> Unit, onSaved: () -> Unit, onEnterKey: () -> Unit) {
    composable(SCAN_ROUTE) {
        ScanRoute(onBack = onBack, onSaved = onSaved, onEnterKey = onEnterKey)
    }
}

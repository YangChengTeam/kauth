package com.yangfeisg.kauth.feature.setup

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SETUP_ROUTE = "setup"

fun NavGraphBuilder.setupScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    composable(SETUP_ROUTE) {
        SetupRoute(onBack = onBack, onSaved = onSaved)
    }
}

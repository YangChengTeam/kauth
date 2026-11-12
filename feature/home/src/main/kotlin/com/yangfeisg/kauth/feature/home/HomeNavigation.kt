package com.yangfeisg.kauth.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val HOME_ROUTE = "home"

/** [onExit] is called once the user has confirmed leaving the app. */
fun NavGraphBuilder.homeScreen(onScanQr: () -> Unit, onEnterKey: () -> Unit, onExit: () -> Unit) {
    composable(HOME_ROUTE) {
        HomeRoute(onScanQr = onScanQr, onEnterKey = onEnterKey, onExit = onExit)
    }
}

package com.yangfeisg.kauth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.yangfeisg.kauth.feature.home.HOME_ROUTE
import com.yangfeisg.kauth.feature.home.homeScreen
import com.yangfeisg.kauth.feature.scan.SCAN_ROUTE
import com.yangfeisg.kauth.feature.scan.scanScreen
import com.yangfeisg.kauth.feature.setup.SETUP_ROUTE
import com.yangfeisg.kauth.feature.setup.setupScreen

/** The app's three screens. Features do not know each other; the hops between them live here. */
@Composable
fun KauthNavHost(onExit: () -> Unit) {
    val navController = rememberNavController()
    val backToHome: () -> Unit = { navController.popBackStack(HOME_ROUTE, inclusive = false) }

    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        homeScreen(
            onScanQr = { navController.navigate(SCAN_ROUTE) },
            onEnterKey = { navController.navigate(SETUP_ROUTE) },
            onExit = onExit,
        )
        setupScreen(onBack = backToHome, onSaved = backToHome)
        scanScreen(
            onBack = backToHome,
            onSaved = backToHome,
            onEnterKey = { navController.navigate(SETUP_ROUTE) { popUpTo(HOME_ROUTE) } },
        )
    }
}

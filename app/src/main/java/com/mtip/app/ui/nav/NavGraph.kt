package com.mtip.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mtip.app.domain.WalletRepository
import com.mtip.app.ui.claim.ClaimScreen
import com.mtip.app.ui.create.WalletListScreen
import com.mtip.app.ui.onboarding.OnboardingScreen
import com.mtip.app.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Wallets : Screen("wallets")
    object Claim : Screen("claim")
    object Settings : Screen("settings")
}

@Composable
fun MTipNavHost(nav: NavHostController, repo: WalletRepository) {
    NavHost(nav, startDestination = Screen.Onboarding.route) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onCreate = { nav.navigate(Screen.Wallets.route) },
                onClaim = { nav.navigate(Screen.Claim.route) },
                onSettings = { nav.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Wallets.route) {
            WalletListScreen(repo) { nav.popBackStack() }
        }
        composable(Screen.Claim.route) {
            ClaimScreen(repo) { nav.popBackStack() }
        }
        composable(Screen.Settings.route) {
            SettingsScreen { nav.popBackStack() }
        }
    }
}
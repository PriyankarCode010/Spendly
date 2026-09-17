package com.spendly.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spendly.app.feature.ai.AiAssistantScreen
import com.spendly.app.feature.analytics.AnalyticsScreen
import com.spendly.app.feature.auth.SignInScreen
import com.spendly.app.feature.auth.SignUpScreen
import com.spendly.app.feature.bmi.BmiScreen
import com.spendly.app.feature.budgets.BudgetsScreen
import com.spendly.app.feature.calendar.CalendarScreen
import com.spendly.app.feature.dashboard.DashboardScreen
import com.spendly.app.feature.goals.GoalsScreen
import com.spendly.app.feature.payments.PaymentsScreen
import com.spendly.app.feature.planner.PlannerScreen
import com.spendly.app.feature.profile.FinancialSetupScreen
import com.spendly.app.feature.subscriptions.SubscriptionsScreen
import com.spendly.app.feature.transactions.TransactionsScreen

object Routes {
    const val SPLASH = "splash"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val FINANCIAL_SETUP = "financial_setup"
    const val DASHBOARD = "dashboard"
    const val PAYMENTS = "payments"
    const val TRANSACTIONS = "transactions"
    const val BUDGETS = "budgets"
    const val ANALYTICS = "analytics"
    const val SUBSCRIPTIONS = "subscriptions"
    const val GOALS = "goals"
    const val PLANNER = "planner"
    const val CALENDAR = "calendar"
    const val BMI = "bmi"
    const val AI = "ai"
}

@Composable
fun SpendlyNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            val appStartViewModel: AppStartViewModel = hiltViewModel()
            val destination by appStartViewModel.startDestination.collectAsStateWithLifecycle()

            when (destination) {
                StartDestination.Loading -> LoadingScreen()
                StartDestination.SignedOut -> NavigateOnce(navController, Routes.SIGN_IN, Routes.SPLASH)
                StartDestination.NeedsFinancialSetup -> NavigateOnce(navController, Routes.FINANCIAL_SETUP, Routes.SPLASH)
                StartDestination.Ready -> NavigateOnce(navController, Routes.DASHBOARD, Routes.SPLASH)
            }
        }

        composable(Routes.SIGN_IN) {
            SignInScreen(
                onAuthenticated = { navController.navigate(Routes.SPLASH) { popUpTo(Routes.SIGN_IN) { inclusive = true } } },
                onNavigateToSignUp = { navController.navigate(Routes.SIGN_UP) }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onAuthenticated = { navController.navigate(Routes.SPLASH) { popUpTo(Routes.SIGN_UP) { inclusive = true } } },
                onNavigateToSignIn = { navController.popBackStack() }
            )
        }

        composable(Routes.FINANCIAL_SETUP) {
            FinancialSetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.FINANCIAL_SETUP) { inclusive = true } }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigate = { route -> navController.navigate(route) },
                onSignOut = {
                    navController.navigate(Routes.SPLASH) { popUpTo(0) }
                }
            )
        }

        composable(Routes.PAYMENTS) { PaymentsScreen() }
        composable(Routes.TRANSACTIONS) { TransactionsScreen() }
        composable(Routes.BUDGETS) { BudgetsScreen() }
        composable(Routes.ANALYTICS) { AnalyticsScreen() }
        composable(Routes.SUBSCRIPTIONS) { SubscriptionsScreen() }
        composable(Routes.GOALS) { GoalsScreen() }
        composable(Routes.PLANNER) { PlannerScreen() }
        composable(Routes.CALENDAR) { CalendarScreen() }
        composable(Routes.BMI) { BmiScreen() }
        composable(Routes.AI) { AiAssistantScreen() }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NavigateOnce(navController: NavHostController, target: String, from: String) {
    androidx.compose.runtime.LaunchedEffect(target) {
        navController.navigate(target) { popUpTo(from) { inclusive = true } }
    }
}

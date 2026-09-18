package com.spendly.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spendly.app.feature.ai.AiAssistantScreen
import com.spendly.app.feature.analytics.AnalyticsScreen
import com.spendly.app.feature.bmi.BmiScreen
import com.spendly.app.feature.budgets.BudgetsScreen
import com.spendly.app.feature.calendar.CalendarScreen
import com.spendly.app.feature.dashboard.DashboardScreen
import com.spendly.app.feature.goals.GoalsScreen
import com.spendly.app.feature.payments.PaymentsScreen
import com.spendly.app.feature.planner.PlannerScreen
import com.spendly.app.feature.subscriptions.SubscriptionsScreen
import com.spendly.app.feature.transactions.TransactionsScreen
import kotlinx.coroutines.launch

private data class DrawerDestination(val label: String, val route: String, val icon: ImageVector)

private val drawerDestinations = listOf(
    DrawerDestination("Dashboard", Routes.DASHBOARD, Icons.Filled.Home),
    DrawerDestination("Scan & Pay", Routes.PAYMENTS, Icons.Filled.QrCodeScanner),
    DrawerDestination("Transactions", Routes.TRANSACTIONS, Icons.AutoMirrored.Filled.ReceiptLong),
    DrawerDestination("Budgets", Routes.BUDGETS, Icons.Filled.PieChart),
    DrawerDestination("Analytics", Routes.ANALYTICS, Icons.Filled.Insights),
    DrawerDestination("Subscriptions", Routes.SUBSCRIPTIONS, Icons.Filled.Subscriptions),
    DrawerDestination("Goals", Routes.GOALS, Icons.Filled.Flag),
    DrawerDestination("Deal Planner", Routes.PLANNER, Icons.Filled.Savings),
    DrawerDestination("Calendar", Routes.CALENDAR, Icons.Filled.CalendarMonth),
    DrawerDestination("BMI Calculator", Routes.BMI, Icons.Filled.MonitorWeight),
    DrawerDestination("AI Assistant", Routes.AI, Icons.Filled.SmartToy)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(onSignOut: () -> Unit) {
    val innerNavController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val backStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull { dest ->
        drawerDestinations.any { it.route == dest.route }
    }?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
                    Text("Spendly", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Your safe-to-spend, made simple",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                drawerDestinations.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.label) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        selected = currentRoute == destination.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            innerNavController.navigate(destination.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Sign out") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSignOut()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(drawerDestinations.find { it.route == currentRoute }?.label ?: "Spendly") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = innerNavController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(padding),
                enterTransition = crossfadeEnter,
                exitTransition = crossfadeExit,
                popEnterTransition = crossfadeEnter,
                popExitTransition = crossfadeExit
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(onQuickAction = { route -> innerNavController.navigate(route) })
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
    }
}

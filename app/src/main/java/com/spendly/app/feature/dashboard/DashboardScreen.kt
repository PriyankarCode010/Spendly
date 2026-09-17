package com.spendly.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class DashboardDestination(val label: String, val route: String)

val dashboardDestinations = listOf(
    DashboardDestination("Scan & Pay", "payments"),
    DashboardDestination("Transactions", "transactions"),
    DashboardDestination("Budgets", "budgets"),
    DashboardDestination("Analytics", "analytics"),
    DashboardDestination("Subscriptions", "subscriptions"),
    DashboardDestination("Goals", "goals"),
    DashboardDestination("Deal Planner", "planner"),
    DashboardDestination("Calendar", "calendar"),
    DashboardDestination("BMI Calculator", "bmi"),
    DashboardDestination("AI Assistant", "ai")
)

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                Text(text = "Good to see you", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Safe to spend", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "Available once the budget engine ships (Phase 3)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Balance", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = profile?.let { "${it.currency} ${it.currentBalance}" } ?: "--",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            items(dashboardDestinations) { destination ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    TextButton(
                        onClick = { onNavigate(destination.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = destination.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out")
                }
            }
        }
    }
}

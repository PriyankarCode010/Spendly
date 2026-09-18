package com.spendly.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.app.domain.engine.DashboardWarningLevel
import com.spendly.app.navigation.Routes

private data class QuickAction(val label: String, val route: String, val icon: ImageVector)

private val quickActions = listOf(
    QuickAction("Scan & Pay", Routes.PAYMENTS, Icons.Filled.QrCodeScanner),
    QuickAction("Transactions", Routes.TRANSACTIONS, Icons.AutoMirrored.Filled.ReceiptLong),
    QuickAction("Budgets", Routes.BUDGETS, Icons.Filled.PieChart),
    QuickAction("Analytics", Routes.ANALYTICS, Icons.Filled.Insights)
)

@Composable
fun DashboardScreen(
    onQuickAction: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(text = "Good to see you", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Here's where things stand today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            val cardColor = when (uiState.warningLevel) {
                DashboardWarningLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                DashboardWarningLevel.WARNING -> MaterialTheme.colorScheme.secondaryContainer
                DashboardWarningLevel.NORMAL -> MaterialTheme.colorScheme.primaryContainer
            }
            val onCardColor = when (uiState.warningLevel) {
                DashboardWarningLevel.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
                DashboardWarningLevel.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
                DashboardWarningLevel.NORMAL -> MaterialTheme.colorScheme.onPrimaryContainer
            }
            val subtitle = when (uiState.warningLevel) {
                DashboardWarningLevel.CRITICAL ->
                    "High priority funds (rent, emergency fund, etc.) are now short. Advisory only - review spending."
                DashboardWarningLevel.WARNING ->
                    "Discretionary spending is dipping into medium priority reserves. Advisory only."
                DashboardWarningLevel.NORMAL ->
                    "Your balance minus what's still reserved for high/medium priority budgets. This is advisory, not a spending limit we can enforce."
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Safe to spend",
                        style = MaterialTheme.typography.labelLarge,
                        color = onCardColor
                    )
                    Text(
                        text = uiState.safeToSpend?.let { "${uiState.profile?.currency.orEmpty()} $it" } ?: "--",
                        style = MaterialTheme.typography.headlineLarge,
                        color = onCardColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = onCardColor
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.balance?.let { balance ->
                            "${uiState.profile?.currency.orEmpty()} $balance"
                        } ?: "--",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Upcoming this month",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${uiState.profile?.currency.orEmpty()} ${uiState.upcomingCommitments.total}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Subscriptions due: ${uiState.profile?.currency.orEmpty()} ${uiState.upcomingCommitments.subscriptionAmount}  ·  " +
                            "Goal savings: ${uiState.profile?.currency.orEmpty()} ${uiState.upcomingCommitments.goalContributionAmount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Forecast only - not yet spent, and separate from Safe to spend above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(text = "Quick actions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(quickActions) { action ->
                    QuickActionItem(action = action, onClick = { onQuickAction(action.route) })
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(action: QuickAction, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

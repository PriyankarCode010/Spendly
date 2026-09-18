package com.spendly.app.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.app.domain.engine.BudgetStatus
import com.spendly.app.domain.engine.BudgetWarningLevel
import com.spendly.app.domain.engine.CategoryTotal
import com.spendly.app.domain.engine.calculateBudgetWarningLevel
import com.spendly.app.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showRangePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.periodType == AnalyticsPeriodType.MONTH,
                    onClick = { viewModel.selectThisMonth() },
                    label = { Text("This month") }
                )
                FilterChip(
                    selected = uiState.periodType == AnalyticsPeriodType.YEAR,
                    onClick = { viewModel.selectThisYear() },
                    label = { Text("This year") }
                )
                FilterChip(
                    selected = uiState.periodType == AnalyticsPeriodType.CUSTOM,
                    onClick = { showRangePicker = true },
                    label = { Text("Custom") }
                )
            }
        }

        item {
            Text("Summary", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    StatRow("Income", uiState.summary.income, MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow("Expenses", uiState.summary.expenses, MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow("Savings", uiState.summary.savings, MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Savings rate: ${"%.1f".format(uiState.summary.savingsRate)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        uiState.monthComparison?.let { comparison ->
            item {
                Text("Vs last month", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Expenses: ${comparison.currentExpenses} (was ${comparison.previousExpenses})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = comparison.expenseDeltaPercent?.let { "%.1f%% vs last month".format(it) }
                                ?: "No spending recorded last month to compare against",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text("By category", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.categoryTotals.isEmpty()) {
            item {
                Text(
                    "No expenses recorded for this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.categoryTotals, key = { it.categoryName }) { total ->
                CategoryRow(total)
            }
        }

        item {
            Text("Budget vs actual", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.budgetStatuses.isEmpty()) {
            item {
                Text(
                    "No budgets set up yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.budgetStatuses, key = { it.budget.id }) { status ->
                BudgetStatusRow(status, categories)
            }
        }

        item {
            Text("Upcoming commitments", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(uiState.upcomingSubscriptionAmount.toString(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Subscriptions due this period - not yet spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text("Plans", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(uiState.goalContributionAmount.toString(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Required monthly saving across goals - a plan, not an expense",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showRangePicker) {
        val rangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = rangePickerState.selectedStartDateMillis
                    val end = rangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        viewModel.selectCustomRange(start, end)
                    }
                    showRangePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(state = rangePickerState, modifier = Modifier.height(500.dp))
        }
    }
}

@Composable
private fun StatRow(label: String, amount: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(amount.toString(), style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun CategoryRow(total: CategoryTotal) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(total.categoryName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${total.amount} (${"%.1f".format(total.percentOfTotal)}%)",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (total.percentOfTotal / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BudgetStatusRow(status: BudgetStatus, categories: List<Category>) {
    val warningLevel = calculateBudgetWarningLevel(status)
    val barColor = when (warningLevel) {
        BudgetWarningLevel.OVER_BUDGET -> MaterialTheme.colorScheme.error
        BudgetWarningLevel.APPROACHING -> MaterialTheme.colorScheme.tertiary
        BudgetWarningLevel.NORMAL -> MaterialTheme.colorScheme.primary
    }
    val categoryName = categories.find { it.id == status.budget.categoryId }?.name ?: "Uncategorized"
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(categoryName, style = MaterialTheme.typography.bodyLarge)
            Text("${status.spent} / ${status.budget.amount}", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (status.spent / status.budget.amount.coerceAtLeast(0.01)).toFloat().coerceIn(0f, 1f) },
            color = barColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

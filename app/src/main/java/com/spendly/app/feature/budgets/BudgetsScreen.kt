package com.spendly.app.feature.budgets

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.app.domain.engine.BudgetStatus
import com.spendly.app.domain.engine.BudgetWarningLevel
import com.spendly.app.domain.engine.DashboardWarningLevel
import com.spendly.app.domain.engine.calculateBudgetWarningLevel
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.BudgetPeriod
import com.spendly.app.domain.model.BudgetPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    var pendingDelete by remember { mutableStateOf<Budget?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingBudget = null
                showAddSheet = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add budget")
            }
        }
    ) { padding ->
        if (uiState.statuses.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Filled.PieChart, contentDescription = null)
                Spacer(modifier = Modifier.height(12.dp))
                Text("No budgets yet", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    "Tap + to set a budget and priority for a category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.dashboardWarningLevel != DashboardWarningLevel.NORMAL) {
                    item { DashboardWarningBanner(uiState.dashboardWarningLevel) }
                }
                items(uiState.statuses, key = { it.budget.id }) { status ->
                    val categoryName = categories.find { it.id == status.budget.categoryId }?.name ?: "Uncategorized"
                    BudgetRow(
                        status = status,
                        categoryName = categoryName,
                        onClick = {
                            editingBudget = status.budget
                            showAddSheet = true
                        },
                        onDeleteClick = { pendingDelete = status.budget }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, sheetState = sheetState) {
            AddBudgetForm(
                categories = categories,
                initial = editingBudget,
                onSave = { categoryId, amount, priority, period ->
                    val original = editingBudget
                    if (original == null) {
                        viewModel.addBudget(categoryId, amount, priority, period)
                    } else {
                        viewModel.updateBudget(original, categoryId, amount, priority, period)
                    }
                    showAddSheet = false
                }
            )
        }
    }

    pendingDelete?.let { budget ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete budget?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBudget(budget.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DashboardWarningBanner(level: DashboardWarningLevel) {
    val (message, color) = when (level) {
        DashboardWarningLevel.CRITICAL -> "High priority funds are now short - review spending." to MaterialTheme.colorScheme.error
        DashboardWarningLevel.WARNING -> "Discretionary spending is dipping into medium priority funds." to MaterialTheme.colorScheme.secondary
        DashboardWarningLevel.NORMAL -> return
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)), modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(16.dp), color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BudgetRow(
    status: BudgetStatus,
    categoryName: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val warningLevel = calculateBudgetWarningLevel(status)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(categoryName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${priorityLabel(status.budget.priority)} • ${periodLabel(status.budget.period)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${status.spent} / ${status.budget.amount}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete budget", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (status.spent / status.budget.amount.coerceAtLeast(0.01)).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = when (warningLevel) {
                    BudgetWarningLevel.OVER_BUDGET -> MaterialTheme.colorScheme.error
                    BudgetWarningLevel.APPROACHING -> MaterialTheme.colorScheme.secondary
                    BudgetWarningLevel.NORMAL -> MaterialTheme.colorScheme.primary
                }
            )
            if (warningLevel == BudgetWarningLevel.OVER_BUDGET) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Over budget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else if (warningLevel == BudgetWarningLevel.APPROACHING) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Approaching limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun priorityLabel(priority: BudgetPriority): String = when (priority) {
    BudgetPriority.HIGH -> "High priority"
    BudgetPriority.MEDIUM -> "Medium priority"
    BudgetPriority.LOW -> "Low priority"
}

private fun periodLabel(period: BudgetPeriod): String =
    period.name.lowercase().replaceFirstChar(Char::uppercase)

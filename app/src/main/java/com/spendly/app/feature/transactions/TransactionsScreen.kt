package com.spendly.app.feature.transactions

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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.VerificationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
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
                editingTransaction = null
                showAddSheet = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            EmptyTransactionsState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    val categoryName = categories.find { it.id == transaction.categoryId }?.name
                    TransactionRow(
                        transaction = transaction,
                        categoryName = categoryName,
                        onClick = {
                            editingTransaction = transaction
                            showAddSheet = true
                        },
                        onDeleteClick = { pendingDelete = transaction }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState
        ) {
            AddTransactionForm(
                categories = categories,
                initial = editingTransaction,
                onSave = { type, amount, categoryId, merchant, description, transactionDate ->
                    val original = editingTransaction
                    if (original == null) {
                        viewModel.addTransaction(type, amount, categoryId, merchant, description, transactionDate)
                    } else {
                        viewModel.updateTransaction(original, type, amount, categoryId, merchant, description, transactionDate)
                    }
                    showAddSheet = false
                }
            )
        }
    }

    pendingDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(transaction.id)
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
private fun EmptyTransactionsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = "No transactions yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tap + to add your first income or expense",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())

@Composable
private fun TransactionRow(
    transaction: Transaction,
    categoryName: String?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant.ifBlank { categoryName ?: "Transaction" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = listOfNotNull(categoryName, dateFormatter.format(Date(transaction.transactionDate)))
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                statusLabel(transaction.verificationStatus)?.let { label ->
                    Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            val isIncome = transaction.type == TransactionType.INCOME
            Text(
                text = "${if (isIncome) "+" else "-"}${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun statusLabel(status: VerificationStatus): String? = when (status) {
    VerificationStatus.LAUNCHED_UNKNOWN -> "Status unknown - check your UPI app"
    VerificationStatus.LAUNCHED_FAILED -> "Payment failed"
    VerificationStatus.OBSERVED_UNVERIFIED -> "Unverified"
    VerificationStatus.MANUAL_ENTRY, VerificationStatus.LAUNCHED_SUCCESS -> null
}

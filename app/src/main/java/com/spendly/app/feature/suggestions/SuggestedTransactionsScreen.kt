package com.spendly.app.feature.suggestions

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSuggestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

@Composable
fun SuggestedTransactionsScreen(viewModel: SuggestionsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var notificationAccessGranted by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted = isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Automatic detection is best-effort and may miss transactions - manual entry always works and stays the reliable way to record spending.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notificationAccessGranted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (notificationAccessGranted) "Notification access is on" else "Notification access is off",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Spendly only reads notifications from Google Pay and PhonePe to suggest transactions - it never reads your calendar, SMS, or other apps. You can revoke this at any time.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                            Text(if (notificationAccessGranted) "Manage access" else "Grant access")
                        }
                    }
                }
            }

            if (uiState.rows.isEmpty()) {
                item {
                    Text(
                        "No suggested transactions right now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.rows, key = { it.suggestion.id }) { row ->
                    SuggestionCard(
                        row = row,
                        onConfirm = { viewModel.confirm(row.suggestion, categoryId = null) },
                        onUseExisting = { existing -> viewModel.useExisting(row.suggestion, existing.id) },
                        onDismiss = { viewModel.dismiss(row.suggestion) }
                    )
                }
            }
        }
    }
}

private fun isNotificationAccessGranted(context: android.content.Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

@Composable
private fun SuggestionCard(
    row: SuggestionRow,
    onConfirm: () -> Unit,
    onUseExisting: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    val suggestion = row.suggestion
    val canConfirm = suggestion.amount != null && suggestion.direction != null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    suggestion.amount?.let { "%.2f".format(it) } ?: "Amount unknown",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    suggestion.direction?.let { if (it == TransactionDirection.DEBIT) "Paid" else "Received" } ?: "Unknown direction",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                suggestion.merchant ?: "Merchant not detected",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${sourceAppLabel(suggestion.sourcePackage)} - ${dateFormatter.format(Date(suggestion.notificationPostedAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val duplicate = row.likelyDuplicateOf
            if (duplicate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This may already be recorded: %.2f on %s".format(duplicate.amount, dateFormatter.format(Date(duplicate.transactionDate))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onUseExisting(duplicate) }) { Text("Use existing") }
                    TextButton(onClick = onConfirm, enabled = canConfirm) { Text("Record anyway") }
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onConfirm, enabled = canConfirm) { Text("Confirm") }
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                }
            }
        }
    }
}

private fun sourceAppLabel(packageName: String): String = when (packageName) {
    "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
    "com.phonepe.app" -> "PhonePe"
    else -> packageName
}

package com.spendly.app.feature.subscriptions

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
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.app.domain.model.Subscription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }
    var pendingCancel by remember { mutableStateOf<Subscription?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val activeSubscriptions = subscriptions.filter { it.isActive }
    val canceledSubscriptions = subscriptions.filter { !it.isActive }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingSubscription = null
                showAddSheet = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add subscription")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column {
                            Text("Monthly", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.2f".format(viewModel.monthlyTotal), style = MaterialTheme.typography.titleLarge)
                        }
                        Column {
                            Text("Yearly", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.2f".format(viewModel.yearlyTotal), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }

            if (activeSubscriptions.isEmpty() && canceledSubscriptions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Filled.Subscriptions, contentDescription = null)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No subscriptions tracked yet", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(activeSubscriptions, key = { it.id }) { subscription ->
                    SubscriptionRow(
                        subscription = subscription,
                        onClick = {
                            editingSubscription = subscription
                            showAddSheet = true
                        },
                        onCancelClick = { pendingCancel = subscription }
                    )
                }
                if (canceledSubscriptions.isNotEmpty()) {
                    item {
                        Text(
                            "Canceled",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(canceledSubscriptions, key = { it.id }) { subscription ->
                        SubscriptionRow(subscription = subscription, onClick = {}, onCancelClick = null)
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, sheetState = sheetState) {
            AddSubscriptionForm(
                initial = editingSubscription,
                categories = categories,
                onSave = { name, amount, frequency, nextPaymentDate, categoryId ->
                    val original = editingSubscription
                    if (original == null) {
                        viewModel.addSubscription(name, amount, frequency, nextPaymentDate, categoryId)
                    } else {
                        viewModel.updateSubscription(original, name, amount, frequency, nextPaymentDate, categoryId)
                    }
                    showAddSheet = false
                }
            )
        }
    }

    pendingCancel?.let { subscription ->
        AlertDialog(
            onDismissRequest = { pendingCancel = null },
            title = { Text("Cancel subscription?") },
            text = { Text("This stops future reminders but keeps its history. It won't be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelSubscription(subscription.id)
                    pendingCancel = null
                }) { Text("Cancel subscription") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancel = null }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun SubscriptionRow(
    subscription: Subscription,
    onClick: () -> Unit,
    onCancelClick: (() -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(enabled = onCancelClick != null, onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = if (subscription.isActive) FontStyle.Normal else FontStyle.Italic,
                    color = if (subscription.isActive) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (subscription.isActive) "Next: ${dateFormatter.format(Date(subscription.nextPaymentDate))}" else "Canceled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                Text(
                    "${subscription.amount} / ${subscription.frequency.name.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (onCancelClick != null) {
                    Spacer(modifier = Modifier.height(0.dp))
                    TextButton(onClick = onCancelClick) { Text("Cancel") }
                }
            }
        }
    }
}

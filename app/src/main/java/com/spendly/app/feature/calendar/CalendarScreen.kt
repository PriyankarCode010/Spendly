package com.spendly.app.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.app.core.calendar.buildCalendarInsertIntent
import com.spendly.app.domain.engine.nextOccurrenceAfter
import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.model.Subscription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
private const val DEFAULT_EVENT_DURATION_MILLIS = 30L * 60 * 1000

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Add a reminder to your device calendar for a subscription's next payment or a goal's target date. Spendly never reads your calendar back - this is one-way only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text("Subscriptions", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.subscriptions.isEmpty()) {
            item {
                Text(
                    "No active subscriptions to remind about",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.subscriptions, key = { it.id }) { subscription ->
                SubscriptionExportRow(
                    subscription = subscription,
                    alreadyExported = subscription.id in uiState.exportedSubscriptionIds,
                    onExport = {
                        val due = nextOccurrenceAfter(subscription, System.currentTimeMillis())
                        val intent = buildCalendarInsertIntent(
                            title = "${subscription.name} payment due",
                            description = "Spendly reminder for the ${subscription.name} subscription.",
                            beginMillis = due,
                            endMillis = due + DEFAULT_EVENT_DURATION_MILLIS
                        )
                        context.startActivity(intent)
                        viewModel.markSubscriptionExported(subscription.id)
                    }
                )
            }
        }

        item {
            Text("Goals", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.goals.isEmpty()) {
            item {
                Text(
                    "No goals with a target date yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.goals, key = { it.id }) { goal ->
                GoalExportRow(
                    goal = goal,
                    alreadyExported = goal.id in uiState.exportedGoalIds,
                    onExport = {
                        val targetDate = goal.targetDate
                        if (targetDate != null) {
                            val intent = buildCalendarInsertIntent(
                                title = "${goal.name} target date",
                                description = "Spendly reminder for your ${goal.name} goal.",
                                beginMillis = targetDate,
                                endMillis = targetDate + TimeUnit.DAYS.toMillis(1)
                            )
                            context.startActivity(intent)
                            viewModel.markGoalExported(goal.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SubscriptionExportRow(subscription: Subscription, alreadyExported: Boolean, onExport: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(subscription.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Due: ${dateFormatter.format(Date(nextOccurrenceAfter(subscription, System.currentTimeMillis())))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onExport) {
                Text(if (alreadyExported) "Re-add to calendar" else "Add to calendar")
            }
        }
    }
}

@Composable
private fun GoalExportRow(goal: Goal, alreadyExported: Boolean, onExport: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(goal.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Target: ${goal.targetDate?.let { dateFormatter.format(Date(it)) } ?: "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onExport) {
                Text(if (alreadyExported) "Re-add to calendar" else "Add to calendar")
            }
        }
    }
}

package com.spendly.app.feature.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.BudgetPeriod
import com.spendly.app.domain.model.BudgetPriority
import com.spendly.app.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetForm(
    categories: List<Category>,
    initial: Budget? = null,
    onSave: (categoryId: String?, amount: Double, priority: BudgetPriority, period: BudgetPeriod) -> Unit
) {
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: BudgetPriority.MEDIUM) }
    var period by remember { mutableStateOf(initial?.period ?: BudgetPeriod.MONTHLY) }
    var selectedCategory by remember { mutableStateOf(categories.find { it.id == initial?.categoryId }) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(if (initial == null) "Add budget" else "Edit budget", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = categoryMenuExpanded,
            onExpandedChange = { categoryMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoryMenuExpanded,
                onDismissRequest = { categoryMenuExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategory = category
                            categoryMenuExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Period", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BudgetPeriod.entries.forEach { p ->
                FilterChip(
                    selected = period == p,
                    onClick = { period = p },
                    label = { Text(p.name.lowercase().replaceFirstChar(Char::uppercase)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Priority", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BudgetPriority.entries.forEach { p ->
                FilterChip(
                    selected = priority == p,
                    onClick = { priority = p },
                    label = { Text(p.name.lowercase().replaceFirstChar(Char::uppercase)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                isSaving = true
                onSave(selectedCategory?.id, amount.toDoubleOrNull() ?: 0.0, priority, period)
            },
            enabled = !isSaving && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (initial == null) "Save" else "Save changes")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

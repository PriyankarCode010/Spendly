package com.spendly.app.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionForm(
    categories: List<Category>,
    initial: Transaction? = null,
    onSave: (type: TransactionType, amount: Double, categoryId: String?, merchant: String, description: String, transactionDate: Long) -> Unit
) {
    var type by remember { mutableStateOf(initial?.type ?: TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(initial?.merchant ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var transactionDate by remember { mutableStateOf(initial?.transactionDate ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedCategory by remember {
        mutableStateOf(categories.find { it.id == initial?.categoryId })
    }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(if (initial == null) "Add transaction" else "Edit transaction", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == TransactionType.EXPENSE,
                onClick = { type = TransactionType.EXPENSE },
                label = { Text("Expense") }
            )
            FilterChip(
                selected = type == TransactionType.INCOME,
                onClick = { type = TransactionType.INCOME },
                label = { Text("Income") }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

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
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
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

        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Date: ${dateFormatter.format(Date(transactionDate))}")
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text("Merchant") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                isSaving = true
                onSave(type, amount.toDoubleOrNull() ?: 0.0, selectedCategory?.id, merchant, description, transactionDate)
            },
            enabled = !isSaving && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (initial == null) "Save" else "Save changes")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = transactionDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { transactionDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

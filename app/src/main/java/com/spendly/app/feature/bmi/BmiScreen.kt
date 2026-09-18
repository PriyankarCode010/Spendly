package com.spendly.app.feature.bmi

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.app.domain.engine.BmiCategory
import com.spendly.app.domain.engine.calculateBmi
import com.spendly.app.domain.engine.classifyBmi
import com.spendly.app.domain.model.BmiEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@Composable
fun BmiScreen(viewModel: BmiViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("BMI Calculator", style = MaterialTheme.typography.titleLarge)
                Text(
                    "A standalone wellness check - not used in any budget or spending calculation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = { Text("Height (cm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                val height = heightInput.toDoubleOrNull() ?: 0.0
                val weight = weightInput.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        viewModel.addEntry(height, weight)
                        heightInput = ""
                        weightInput = ""
                    },
                    enabled = height > 0.0 && weight > 0.0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }

            val latest = history.firstOrNull()
            if (latest != null) {
                item {
                    val bmi = calculateBmi(latest.heightCm, latest.weightKg)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Latest", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.1f".format(bmi), style = MaterialTheme.typography.headlineLarge)
                            Text(classifyBmi(bmi).label(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Text("History", style = MaterialTheme.typography.titleMedium)
                }
                items(history, key = { it.id }) { entry ->
                    HistoryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: BmiEntry) {
    val bmi = calculateBmi(entry.heightCm, entry.weightKg)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(dateFormatter.format(Date(entry.recordedAt)), style = MaterialTheme.typography.bodyMedium)
        Text("%.1f - %s".format(bmi, classifyBmi(bmi).label()), style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(modifier = Modifier.height(4.dp))
}

private fun BmiCategory.label(): String = when (this) {
    BmiCategory.UNDERWEIGHT -> "Underweight"
    BmiCategory.NORMAL -> "Normal"
    BmiCategory.OVERWEIGHT -> "Overweight"
    BmiCategory.OBESE -> "Obese"
}

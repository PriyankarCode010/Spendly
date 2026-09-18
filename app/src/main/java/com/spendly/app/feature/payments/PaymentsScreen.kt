package com.spendly.app.feature.payments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.VerificationStatus

private sealed class PaymentStep {
    data object Scanning : PaymentStep()
    data class Confirming(val request: UpiPaymentRequest) : PaymentStep()
    data class Done(val status: VerificationStatus, val amount: Double, val payee: String) : PaymentStep()
}

@Composable
fun PaymentsScreen(viewModel: PaymentsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()

    var step by remember { mutableStateOf<PaymentStep>(PaymentStep.Scanning) }
    var scanAttempt by remember { mutableIntStateOf(0) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingRequest by remember { mutableStateOf<UpiPaymentRequest?>(null) }
    var pendingAmount by remember { mutableStateOf(0.0) }
    var pendingCategoryId by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val upiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val request = pendingRequest
        if (request != null) {
            val status = resolveVerificationStatus(result.resultCode, result.data)
            viewModel.recordPayment(request, pendingAmount, pendingCategoryId, status)
            step = PaymentStep.Done(status, pendingAmount, request.payeeName ?: request.payeeVpa)
        }
    }

    when (val currentStep = step) {
        PaymentStep.Scanning -> {
            if (!hasCameraPermission) {
                CameraPermissionRequest(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
            } else {
                key(scanAttempt) {
                    QrScannerView(
                        onQrCodeDetected = { raw ->
                            val request = parseUpiUri(raw)
                            if (request == null) {
                                Toast.makeText(context, "That's not a UPI payment QR code", Toast.LENGTH_SHORT).show()
                                scanAttempt++
                            } else {
                                pendingRequest = request
                                pendingAmount = request.amount ?: 0.0
                                step = PaymentStep.Confirming(request)
                            }
                        }
                    )
                }
            }
        }

        is PaymentStep.Confirming -> {
            ConfirmPaymentScreen(
                request = currentStep.request,
                categories = categories,
                onAmountChange = { pendingAmount = it },
                onCategorySelected = { pendingCategoryId = it },
                onPay = { app ->
                    val intent = Intent(Intent.ACTION_VIEW, buildUpiPayIntentUri(currentStep.request, pendingAmount))
                    intent.setPackage(app.packageName)
                    runCatching { upiLauncher.launch(intent) }
                        .onFailure { Toast.makeText(context, "Couldn't open ${app.label}", Toast.LENGTH_SHORT).show() }
                },
                onCancel = {
                    pendingRequest = null
                    step = PaymentStep.Scanning
                    scanAttempt++
                }
            )
        }

        is PaymentStep.Done -> {
            PaymentResultScreen(
                status = currentStep.status,
                amount = currentStep.amount,
                payee = currentStep.payee,
                onDone = {
                    pendingRequest = null
                    step = PaymentStep.Scanning
                    scanAttempt++
                }
            )
        }
    }
}

@Composable
private fun CameraPermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = null)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Camera access is needed to scan UPI QR codes",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant camera access") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmPaymentScreen(
    request: UpiPaymentRequest,
    categories: List<Category>,
    onAmountChange: (Double) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPay: (UpiApp) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val upiApps = remember { resolveInstalledUpiApps(context) }
    var amountText by remember { mutableStateOf(request.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Paying", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(request.payeeName ?: request.payeeVpa, style = MaterialTheme.typography.titleLarge)
                if (request.payeeName != null) {
                    Text(request.payeeVpa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = {
                amountText = it
                onAmountChange(it.toDoubleOrNull() ?: 0.0)
            },
            label = { Text("Amount") },
            singleLine = true,
            readOnly = request.amount != null,
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
                            onCategorySelected(category.id)
                            categoryMenuExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Text("Pay with", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (upiApps.isEmpty()) {
            Text(
                "No UPI apps found on this device. Install GPay, PhonePe, or Paytm to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(upiApps) { app ->
                    OutlinedButton(
                        onClick = { onPay(app) },
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(app.label)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You'll confirm and complete this payment inside your UPI app. Spendly only records the result afterwards.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
private fun PaymentResultScreen(
    status: VerificationStatus,
    amount: Double,
    payee: String,
    onDone: () -> Unit
) {
    val (icon, title, message) = when (status) {
        VerificationStatus.LAUNCHED_SUCCESS -> Triple(
            Icons.Filled.CheckCircle,
            "Payment successful",
            "Paid $amount to $payee."
        )
        VerificationStatus.LAUNCHED_FAILED -> Triple(
            Icons.Filled.Error,
            "Payment failed",
            "Your payment to $payee didn't go through."
        )
        else -> Triple(
            Icons.Filled.HelpOutline,
            "Payment status unknown",
            "We couldn't confirm whether this payment to $payee succeeded. Check your UPI app, and correct this transaction later if needed."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}

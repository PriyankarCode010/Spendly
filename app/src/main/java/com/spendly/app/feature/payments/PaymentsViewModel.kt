package com.spendly.app.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.VerificationStatus
import com.spendly.app.domain.usecase.AddTransactionUseCase
import com.spendly.app.domain.usecase.GetCategoriesUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    val categories: StateFlow<List<Category>> = getCurrentUserUseCase.currentUser
        .flatMapLatest { user -> user?.let { getCategoriesUseCase(it.id) } ?: MutableStateFlow(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordPayment(
        request: UpiPaymentRequest,
        amount: Double,
        categoryId: String?,
        status: VerificationStatus
    ) {
        viewModelScope.launch {
            val userId = getCurrentUserUseCase() ?: return@launch
            val now = System.currentTimeMillis()
            addTransactionUseCase(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    userId = userId.id,
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    categoryId = categoryId,
                    merchant = request.payeeName ?: request.payeeVpa,
                    description = request.note.orEmpty(),
                    transactionDate = now,
                    verificationStatus = status,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}

package com.spendly.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.VerificationStatus
import com.spendly.app.domain.usecase.AddTransactionUseCase
import com.spendly.app.domain.usecase.DeleteTransactionUseCase
import com.spendly.app.domain.usecase.GetCategoriesUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetTransactionsUseCase
import com.spendly.app.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {

    private var currentUserId: String? = null

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val categories: StateFlow<List<Category>> = getCurrentUserUseCase.currentUser
        .flatMapLatest { user -> user?.let { getCategoriesUseCase(it.id) } ?: MutableStateFlow(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        getCurrentUserUseCase.currentUser
            .onEach { currentUserId = it?.id }
            .flatMapLatest { user -> user?.let { getTransactionsUseCase(it.id) } ?: MutableStateFlow(emptyList()) }
            .onEach { _transactions.value = it }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        categoryId: String?,
        merchant: String,
        description: String,
        transactionDate: Long
    ) {
        val userId = currentUserId ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val result = addTransactionUseCase(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    merchant = merchant,
                    description = description,
                    transactionDate = transactionDate,
                    verificationStatus = VerificationStatus.MANUAL_ENTRY,
                    createdAt = now,
                    updatedAt = now
                )
            )
            reportIfError(result)
        }
    }

    fun updateTransaction(
        original: Transaction,
        type: TransactionType,
        amount: Double,
        categoryId: String?,
        merchant: String,
        description: String,
        transactionDate: Long
    ) {
        viewModelScope.launch {
            val result = updateTransactionUseCase(
                original.copy(
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    merchant = merchant,
                    description = description,
                    transactionDate = transactionDate,
                    updatedAt = System.currentTimeMillis()
                )
            )
            reportIfError(result)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            reportIfError(deleteTransactionUseCase(id))
        }
    }

    private fun reportIfError(result: AppResult<Unit>) {
        if (result is AppResult.Error) {
            _errorMessage.value = result.message
        }
    }
}

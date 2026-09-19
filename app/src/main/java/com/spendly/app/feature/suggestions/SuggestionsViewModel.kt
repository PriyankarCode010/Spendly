package com.spendly.app.feature.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.engine.findLikelyDuplicateTransaction
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionSuggestion
import com.spendly.app.domain.usecase.ConfirmSuggestionUseCase
import com.spendly.app.domain.usecase.DismissSuggestionUseCase
import com.spendly.app.domain.usecase.GetCategoriesUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetPendingSuggestionsUseCase
import com.spendly.app.domain.usecase.GetTransactionsUseCase
import com.spendly.app.domain.usecase.LinkSuggestionToExistingTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuggestionRow(
    val suggestion: TransactionSuggestion,
    val likelyDuplicateOf: Transaction?
)

data class SuggestionsUiState(
    val rows: List<SuggestionRow> = emptyList(),
    val categories: List<Category> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getPendingSuggestionsUseCase: GetPendingSuggestionsUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val confirmSuggestionUseCase: ConfirmSuggestionUseCase,
    private val linkSuggestionToExistingTransactionUseCase: LinkSuggestionToExistingTransactionUseCase,
    private val dismissSuggestionUseCase: DismissSuggestionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestionsUiState())
    val uiState: StateFlow<SuggestionsUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        getCurrentUserUseCase.currentUser
            .flatMapLatest { user ->
                if (user == null) return@flatMapLatest MutableStateFlow(SuggestionsUiState())
                combine(
                    getPendingSuggestionsUseCase(user.id),
                    getTransactionsUseCase(user.id),
                    getCategoriesUseCase(user.id)
                ) { pending, transactions, categories ->
                    SuggestionsUiState(
                        rows = pending.map { suggestion ->
                            SuggestionRow(
                                suggestion = suggestion,
                                likelyDuplicateOf = findLikelyDuplicateTransaction(suggestion, transactions)
                            )
                        },
                        categories = categories
                    )
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun confirm(suggestion: TransactionSuggestion, categoryId: String?) {
        viewModelScope.launch {
            reportIfError(confirmSuggestionUseCase(suggestion, categoryId))
        }
    }

    fun useExisting(suggestion: TransactionSuggestion, existingTransactionId: String) {
        viewModelScope.launch {
            reportIfError(linkSuggestionToExistingTransactionUseCase(suggestion.id, existingTransactionId))
        }
    }

    fun dismiss(suggestion: TransactionSuggestion) {
        viewModelScope.launch {
            reportIfError(dismissSuggestionUseCase(suggestion.id))
        }
    }

    private fun reportIfError(result: AppResult<Unit>) {
        if (result is AppResult.Error) {
            _errorMessage.value = result.message
        }
    }
}

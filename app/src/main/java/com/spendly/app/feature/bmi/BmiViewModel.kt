package com.spendly.app.feature.bmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.BmiEntry
import com.spendly.app.domain.usecase.AddBmiEntryUseCase
import com.spendly.app.domain.usecase.GetBmiHistoryUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BmiViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getBmiHistoryUseCase: GetBmiHistoryUseCase,
    private val addBmiEntryUseCase: AddBmiEntryUseCase
) : ViewModel() {

    private var currentUserId: String? = null

    private val _history = MutableStateFlow<List<BmiEntry>>(emptyList())
    val history: StateFlow<List<BmiEntry>> = _history.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        getCurrentUserUseCase.currentUser
            .onEach { currentUserId = it?.id }
            .flatMapLatest { user -> user?.let { getBmiHistoryUseCase(it.id) } ?: MutableStateFlow(emptyList()) }
            .onEach { _history.value = it }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addEntry(heightCm: Double, weightKg: Double) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val result = addBmiEntryUseCase(
                BmiEntry(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    recordedAt = System.currentTimeMillis()
                )
            )
            if (result is AppResult.Error) {
                _errorMessage.value = result.message
            }
        }
    }
}

package com.spendly.app.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Subscription
import com.spendly.app.domain.model.SubscriptionFrequency
import com.spendly.app.domain.usecase.AddSubscriptionUseCase
import com.spendly.app.domain.usecase.CancelSubscriptionUseCase
import com.spendly.app.domain.usecase.GetCategoriesUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetSubscriptionsUseCase
import com.spendly.app.domain.usecase.UpdateSubscriptionUseCase
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
class SubscriptionsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getSubscriptionsUseCase: GetSubscriptionsUseCase,
    private val addSubscriptionUseCase: AddSubscriptionUseCase,
    private val updateSubscriptionUseCase: UpdateSubscriptionUseCase,
    private val cancelSubscriptionUseCase: CancelSubscriptionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private var currentUserId: String? = null

    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val categories: StateFlow<List<Category>> = getCurrentUserUseCase.currentUser
        .flatMapLatest { user -> user?.let { getCategoriesUseCase(it.id) } ?: MutableStateFlow(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotal: Double
        get() = _subscriptions.value.filter { it.isActive }.sumOf { it.amount * it.frequency.perYear / 12.0 }

    val yearlyTotal: Double
        get() = _subscriptions.value.filter { it.isActive }.sumOf { it.amount * it.frequency.perYear }

    init {
        getCurrentUserUseCase.currentUser
            .onEach { currentUserId = it?.id }
            .flatMapLatest { user -> user?.let { getSubscriptionsUseCase(it.id) } ?: MutableStateFlow(emptyList()) }
            .onEach { _subscriptions.value = it }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addSubscription(
        name: String,
        amount: Double,
        frequency: SubscriptionFrequency,
        nextPaymentDate: Long,
        categoryId: String?
    ) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            reportIfError(
                addSubscriptionUseCase(
                    Subscription(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        name = name,
                        amount = amount,
                        frequency = frequency,
                        nextPaymentDate = nextPaymentDate,
                        categoryId = categoryId,
                        isActive = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    fun updateSubscription(
        original: Subscription,
        name: String,
        amount: Double,
        frequency: SubscriptionFrequency,
        nextPaymentDate: Long,
        categoryId: String?
    ) {
        viewModelScope.launch {
            reportIfError(
                updateSubscriptionUseCase(
                    original.copy(
                        name = name,
                        amount = amount,
                        frequency = frequency,
                        nextPaymentDate = nextPaymentDate,
                        categoryId = categoryId,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    /** Cancellation is a soft-delete: isActive = false, record retained for history. */
    fun cancelSubscription(id: String) {
        viewModelScope.launch {
            reportIfError(cancelSubscriptionUseCase(id))
        }
    }

    private fun reportIfError(result: AppResult<Unit>) {
        if (result is AppResult.Error) {
            _errorMessage.value = result.message
        }
    }
}

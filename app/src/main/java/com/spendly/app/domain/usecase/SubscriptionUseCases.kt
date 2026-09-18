package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Subscription
import com.spendly.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSubscriptionsUseCase @Inject constructor(private val repository: SubscriptionRepository) {
    operator fun invoke(userId: String): Flow<List<Subscription>> = repository.observeAll(userId)
}

class AddSubscriptionUseCase @Inject constructor(private val repository: SubscriptionRepository) {
    suspend operator fun invoke(subscription: Subscription): AppResult<Unit> = repository.addSubscription(subscription)
}

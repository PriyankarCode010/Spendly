package com.spendly.app.domain.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeAll(userId: String): Flow<List<Subscription>>
    suspend fun addSubscription(subscription: Subscription): AppResult<Unit>
}

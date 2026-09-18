package com.spendly.app.domain.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeActive(userId: String): Flow<List<Subscription>>
    fun observeAll(userId: String): Flow<List<Subscription>>
    suspend fun getById(id: String): Subscription?
    suspend fun addSubscription(subscription: Subscription): AppResult<Unit>
    suspend fun updateSubscription(subscription: Subscription): AppResult<Unit>
    suspend fun cancelSubscription(id: String): AppResult<Unit>
}

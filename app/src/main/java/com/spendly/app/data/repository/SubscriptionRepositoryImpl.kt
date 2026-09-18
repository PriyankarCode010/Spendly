package com.spendly.app.data.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.data.local.SubscriptionDao
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import com.spendly.app.domain.model.Subscription
import com.spendly.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) : SubscriptionRepository {

    override fun observeActive(userId: String): Flow<List<Subscription>> =
        subscriptionDao.observeActive(userId).map { list -> list.map { it.toDomain() } }

    override fun observeAll(userId: String): Flow<List<Subscription>> =
        subscriptionDao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Subscription? =
        subscriptionDao.getById(id)?.toDomain()

    override suspend fun addSubscription(subscription: Subscription): AppResult<Unit> = runCatching {
        subscriptionDao.insert(subscription.toEntity())
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to save subscription", it) }
    )

    override suspend fun updateSubscription(subscription: Subscription): AppResult<Unit> = runCatching {
        subscriptionDao.update(subscription.toEntity())
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to update subscription", it) }
    )

    // Cancellation retains the record (isActive = false) - never a hard delete.
    override suspend fun cancelSubscription(id: String): AppResult<Unit> = runCatching {
        val existing = subscriptionDao.getById(id) ?: error("Subscription not found")
        subscriptionDao.update(existing.copy(isActive = false, updatedAt = System.currentTimeMillis()))
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to cancel subscription", it) }
    )
}

package com.spendly.app.data.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.data.local.ProfileDao
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import com.spendly.app.data.remote.ProfileDto
import com.spendly.app.data.remote.SupabaseProfileDataSource
import com.spendly.app.domain.model.Profile
import com.spendly.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val remoteDataSource: SupabaseProfileDataSource
) : ProfileRepository {

    override fun observeProfile(userId: String): Flow<Profile?> =
        profileDao.observe(userId).map { it?.toDomain() }

    override suspend fun getProfile(userId: String): Profile? {
        profileDao.get(userId)?.let { return it.toDomain() }

        return runCatching { remoteDataSource.getProfile(userId) }.getOrNull()?.let { dto ->
            val profile = dto.toDomain()
            profileDao.upsert(profile.toEntity())
            profile
        }
    }

    override suspend fun saveProfile(profile: Profile): AppResult<Unit> {
        profileDao.upsert(profile.toEntity())

        return runCatching {
            remoteDataSource.upsertProfile(profile.toDto())
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = {
                // Cached locally already; remote write will be retried by the
                // sync engine introduced in Phase 2 once multi-record sync exists.
                AppResult.Error(it.message ?: "Saved locally, failed to sync", it)
            }
        )
    }
}

private fun ProfileDto.toDomain(): Profile = Profile(
    userId = id,
    currency = currency,
    monthlyIncome = monthly_income,
    currentBalance = current_balance,
    updatedAt = Instant.parse(updated_at).toEpochMilli()
)

private fun Profile.toDto(): ProfileDto = ProfileDto(
    id = userId,
    currency = currency,
    monthly_income = monthlyIncome,
    current_balance = currentBalance,
    updated_at = Instant.ofEpochMilli(updatedAt).toString()
)

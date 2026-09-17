package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Profile
import com.spendly.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val repository: ProfileRepository) {
    fun observe(userId: String): Flow<Profile?> = repository.observeProfile(userId)
    suspend operator fun invoke(userId: String): Profile? = repository.getProfile(userId)
}

class SaveProfileUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(profile: Profile): AppResult<Unit> = repository.saveProfile(profile)
}

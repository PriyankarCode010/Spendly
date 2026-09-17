package com.spendly.app.domain.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(userId: String): Flow<Profile?>
    suspend fun getProfile(userId: String): Profile?
    suspend fun saveProfile(profile: Profile): AppResult<Unit>
}

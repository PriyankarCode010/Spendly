package com.spendly.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ProfileDto(
    val id: String,
    val currency: String,
    val monthly_income: Double,
    val current_balance: Double,
    val updated_at: String
)

@Singleton
class SupabaseProfileDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    private val table get() = client.postgrest.from("profiles")

    suspend fun getProfile(userId: String): ProfileDto? =
        table.select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull()

    suspend fun upsertProfile(profile: ProfileDto) {
        table.upsert(profile)
    }
}

package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index("userId")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val isDefault: Boolean
)

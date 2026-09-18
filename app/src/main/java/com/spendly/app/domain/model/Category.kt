package com.spendly.app.domain.model

data class Category(
    val id: String,
    val userId: String,
    val name: String,
    val isDefault: Boolean
)

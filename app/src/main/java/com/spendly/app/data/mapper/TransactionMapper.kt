package com.spendly.app.data.mapper

import com.spendly.app.data.local.TransactionEntity
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.VerificationStatus

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    userId = userId,
    type = TransactionType.valueOf(type),
    amount = amount,
    categoryId = categoryId,
    merchant = merchant,
    description = description,
    transactionDate = transactionDate,
    verificationStatus = VerificationStatus.valueOf(verificationStatus),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    userId = userId,
    type = type.name,
    amount = amount,
    categoryId = categoryId,
    merchant = merchant,
    description = description,
    transactionDate = transactionDate,
    verificationStatus = verificationStatus.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)

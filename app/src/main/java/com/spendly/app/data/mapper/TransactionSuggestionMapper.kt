package com.spendly.app.data.mapper

import com.spendly.app.data.local.TransactionSuggestionEntity
import com.spendly.app.domain.model.SuggestionStatus
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSuggestion

fun TransactionSuggestionEntity.toDomain(): TransactionSuggestion = TransactionSuggestion(
    id = id,
    userId = userId,
    sourcePackage = sourcePackage,
    amount = amount,
    direction = direction?.let { TransactionDirection.valueOf(it) },
    merchant = merchant,
    referenceId = referenceId,
    notificationPostedAt = notificationPostedAt,
    status = SuggestionStatus.valueOf(status),
    createdAt = createdAt,
    resultingTransactionId = resultingTransactionId
)

fun TransactionSuggestion.toEntity(): TransactionSuggestionEntity = TransactionSuggestionEntity(
    id = id,
    userId = userId,
    sourcePackage = sourcePackage,
    amount = amount,
    direction = direction?.name,
    merchant = merchant,
    referenceId = referenceId,
    notificationPostedAt = notificationPostedAt,
    status = status.name,
    createdAt = createdAt,
    resultingTransactionId = resultingTransactionId
)

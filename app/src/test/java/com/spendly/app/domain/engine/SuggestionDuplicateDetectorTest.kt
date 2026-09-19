package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSuggestion
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.VerificationStatus
import com.spendly.app.domain.model.SuggestionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.concurrent.TimeUnit

class SuggestionDuplicateDetectorTest {

    private fun suggestion(
        amount: Double? = 250.0,
        direction: TransactionDirection? = TransactionDirection.DEBIT,
        sourcePackage: String = "com.phonepe.app",
        postedAt: Long = 1_000_000L
    ) = TransactionSuggestion(
        id = "sg-${System.nanoTime()}",
        userId = "u1",
        sourcePackage = sourcePackage,
        amount = amount,
        direction = direction,
        merchant = "Test",
        referenceId = null,
        notificationPostedAt = postedAt,
        status = SuggestionStatus.PENDING,
        createdAt = postedAt,
        resultingTransactionId = null
    )

    private fun transaction(
        amount: Double = 250.0,
        type: TransactionType = TransactionType.EXPENSE,
        transactionDate: Long = 1_000_000L
    ) = Transaction(
        id = "t-${System.nanoTime()}",
        userId = "u1",
        type = type,
        amount = amount,
        categoryId = null,
        merchant = "Test",
        description = "",
        transactionDate = transactionDate,
        verificationStatus = VerificationStatus.MANUAL_ENTRY,
        createdAt = transactionDate,
        updatedAt = transactionDate
    )

    @Test
    fun `isDuplicateSuggestion matches same package, direction, amount within window`() {
        val existing = suggestion(postedAt = 1_000_000L)
        val candidate = suggestion(postedAt = 1_000_000L + TimeUnit.MINUTES.toMillis(2))

        assertTrue(isDuplicateSuggestion(candidate, listOf(existing)))
    }

    @Test
    fun `isDuplicateSuggestion does not match outside the window`() {
        val existing = suggestion(postedAt = 1_000_000L)
        val candidate = suggestion(postedAt = 1_000_000L + TimeUnit.MINUTES.toMillis(30))

        assertFalse(isDuplicateSuggestion(candidate, listOf(existing)))
    }

    @Test
    fun `isDuplicateSuggestion does not match a different amount or package`() {
        val existing = suggestion(amount = 250.0, sourcePackage = "com.phonepe.app")
        val differentAmount = suggestion(amount = 300.0, sourcePackage = "com.phonepe.app")
        val differentPackage = suggestion(amount = 250.0, sourcePackage = "com.google.android.apps.nbu.paisa.user")

        assertFalse(isDuplicateSuggestion(differentAmount, listOf(existing)))
        assertFalse(isDuplicateSuggestion(differentPackage, listOf(existing)))
    }

    @Test
    fun `findLikelyDuplicateTransaction matches amount, direction, and time window`() {
        val candidate = suggestion(amount = 250.0, direction = TransactionDirection.DEBIT, postedAt = 1_000_000L)
        val tx = transaction(amount = 250.0, type = TransactionType.EXPENSE, transactionDate = 1_000_000L + TimeUnit.MINUTES.toMillis(3))

        assertEquals(tx.id, findLikelyDuplicateTransaction(candidate, listOf(tx))?.id)
    }

    @Test
    fun `findLikelyDuplicateTransaction does not match wrong direction`() {
        val candidate = suggestion(amount = 250.0, direction = TransactionDirection.DEBIT)
        val tx = transaction(amount = 250.0, type = TransactionType.INCOME)

        assertNull(findLikelyDuplicateTransaction(candidate, listOf(tx)))
    }

    @Test
    fun `findLikelyDuplicateTransaction never matches outside the time window`() {
        val candidate = suggestion(amount = 250.0, postedAt = 1_000_000L)
        val tx = transaction(amount = 250.0, transactionDate = 1_000_000L + TimeUnit.HOURS.toMillis(2))

        assertNull(findLikelyDuplicateTransaction(candidate, listOf(tx)))
    }
}

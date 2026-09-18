package com.spendly.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionAffectsBalanceTest {

    private fun transactionWith(status: VerificationStatus) = Transaction(
        id = "t1",
        userId = "u1",
        type = TransactionType.EXPENSE,
        amount = 100.0,
        categoryId = null,
        merchant = "Test",
        description = "",
        transactionDate = 0L,
        verificationStatus = status,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun `manual entry affects balance`() {
        assertTrue(transactionWith(VerificationStatus.MANUAL_ENTRY).affectsBalance)
    }

    @Test
    fun `launched success affects balance`() {
        assertTrue(transactionWith(VerificationStatus.LAUNCHED_SUCCESS).affectsBalance)
    }

    @Test
    fun `observed unverified affects balance`() {
        assertTrue(transactionWith(VerificationStatus.OBSERVED_UNVERIFIED).affectsBalance)
    }

    @Test
    fun `launched failed does not affect balance`() {
        assertFalse(transactionWith(VerificationStatus.LAUNCHED_FAILED).affectsBalance)
    }

    @Test
    fun `launched unknown does not affect balance`() {
        assertFalse(transactionWith(VerificationStatus.LAUNCHED_UNKNOWN).affectsBalance)
    }
}

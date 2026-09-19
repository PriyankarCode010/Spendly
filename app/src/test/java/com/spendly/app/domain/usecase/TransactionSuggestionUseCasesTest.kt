package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.engine.isDuplicateSuggestion
import com.spendly.app.domain.engine.notification.ParsedSuggestion
import com.spendly.app.domain.model.SuggestionStatus
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSuggestion
import com.spendly.app.domain.model.VerificationStatus
import com.spendly.app.domain.repository.TransactionRepository
import com.spendly.app.domain.repository.TransactionSuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

private class FakeTransactionRepository : TransactionRepository {
    val stored = mutableListOf<Transaction>()
    private val flow = MutableStateFlow<List<Transaction>>(emptyList())

    override fun observeAll(userId: String): Flow<List<Transaction>> = flow
    override suspend fun getById(id: String): Transaction? = stored.find { it.id == id }
    override suspend fun addTransaction(transaction: Transaction): AppResult<Unit> {
        stored.add(transaction)
        flow.value = stored.toList()
        return AppResult.Success(Unit)
    }
    override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteTransaction(id: String): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeSuggestionRepository : TransactionSuggestionRepository {
    val stored = mutableListOf<TransactionSuggestion>()
    private val flow = MutableStateFlow<List<TransactionSuggestion>>(emptyList())

    override fun observePending(userId: String): Flow<List<TransactionSuggestion>> = flow
    override fun observeAll(userId: String): Flow<List<TransactionSuggestion>> = flow

    override suspend fun ingest(suggestion: TransactionSuggestion): AppResult<Unit> {
        val nearbyPending = stored.filter { it.status == SuggestionStatus.PENDING }
        if (!isDuplicateSuggestion(suggestion, nearbyPending)) {
            stored.add(suggestion)
            flow.value = stored.toList()
        }
        return AppResult.Success(Unit)
    }

    override suspend fun markConfirmed(id: String, transactionId: String): AppResult<Unit> {
        val index = stored.indexOfFirst { it.id == id }
        if (index == -1) return AppResult.Error("not found")
        stored[index] = stored[index].copy(status = SuggestionStatus.CONFIRMED, resultingTransactionId = transactionId)
        flow.value = stored.toList()
        return AppResult.Success(Unit)
    }

    override suspend fun markDismissed(id: String): AppResult<Unit> {
        val index = stored.indexOfFirst { it.id == id }
        if (index == -1) return AppResult.Error("not found")
        stored[index] = stored[index].copy(status = SuggestionStatus.DISMISSED)
        flow.value = stored.toList()
        return AppResult.Success(Unit)
    }
}

class TransactionSuggestionUseCasesTest {

    private fun suggestion(
        amount: Double? = 250.0,
        direction: TransactionDirection? = TransactionDirection.DEBIT,
        status: SuggestionStatus = SuggestionStatus.PENDING
    ) = TransactionSuggestion(
        id = UUID.randomUUID().toString(),
        userId = "u1",
        sourcePackage = "com.phonepe.app",
        amount = amount,
        direction = direction,
        merchant = "Cafe",
        referenceId = null,
        notificationPostedAt = 1_000_000L,
        status = status,
        createdAt = 1_000_000L,
        resultingTransactionId = null
    )

    @Test
    fun `ingesting a notification suggestion never creates a Transaction`() = runTest {
        val transactionRepo = FakeTransactionRepository()
        val suggestionRepo = FakeSuggestionRepository()
        val ingest = IngestNotificationSuggestionUseCase(suggestionRepo)

        ingest("u1", "com.phonepe.app", ParsedSuggestion(250.0, TransactionDirection.DEBIT, "Cafe", null), 1_000_000L)

        assertTrue(transactionRepo.stored.isEmpty())
        assertEquals(1, suggestionRepo.stored.size)
        assertEquals(SuggestionStatus.PENDING, suggestionRepo.stored.single().status)
    }

    @Test
    fun `confirming a suggestion creates an OBSERVED_UNVERIFIED transaction and marks the suggestion confirmed`() = runTest {
        val transactionRepo = FakeTransactionRepository()
        val suggestionRepo = FakeSuggestionRepository()
        val confirm = ConfirmSuggestionUseCase(suggestionRepo, AddTransactionUseCase(transactionRepo))
        val original = suggestion()
        suggestionRepo.stored.add(original)

        val result = confirm(original, categoryId = "cat1")

        assertTrue(result is AppResult.Success)
        assertEquals(1, transactionRepo.stored.size)
        assertEquals(VerificationStatus.OBSERVED_UNVERIFIED, transactionRepo.stored.single().verificationStatus)
        assertEquals(SuggestionStatus.CONFIRMED, suggestionRepo.stored.single().status)
        assertEquals(transactionRepo.stored.single().id, suggestionRepo.stored.single().resultingTransactionId)
    }

    @Test
    fun `confirming a suggestion without an amount fails and creates no transaction`() = runTest {
        val transactionRepo = FakeTransactionRepository()
        val suggestionRepo = FakeSuggestionRepository()
        val confirm = ConfirmSuggestionUseCase(suggestionRepo, AddTransactionUseCase(transactionRepo))
        val original = suggestion(amount = null)
        suggestionRepo.stored.add(original)

        val result = confirm(original, categoryId = null)

        assertTrue(result is AppResult.Error)
        assertTrue(transactionRepo.stored.isEmpty())
    }

    @Test
    fun `dismissing a suggestion never creates a transaction`() = runTest {
        val transactionRepo = FakeTransactionRepository()
        val suggestionRepo = FakeSuggestionRepository()
        val dismiss = DismissSuggestionUseCase(suggestionRepo)
        val original = suggestion()
        suggestionRepo.stored.add(original)

        dismiss(original.id)

        assertTrue(transactionRepo.stored.isEmpty())
        assertEquals(SuggestionStatus.DISMISSED, suggestionRepo.stored.single().status)
        assertNull(suggestionRepo.stored.single().resultingTransactionId)
    }

    @Test
    fun `linking to an existing transaction confirms the suggestion without creating a new transaction`() = runTest {
        val transactionRepo = FakeTransactionRepository()
        val suggestionRepo = FakeSuggestionRepository()
        val link = LinkSuggestionToExistingTransactionUseCase(suggestionRepo)
        val original = suggestion()
        suggestionRepo.stored.add(original)

        link(original.id, "existing-tx-id")

        assertTrue(transactionRepo.stored.isEmpty())
        assertEquals(SuggestionStatus.CONFIRMED, suggestionRepo.stored.single().status)
        assertEquals("existing-tx-id", suggestionRepo.stored.single().resultingTransactionId)
    }
}

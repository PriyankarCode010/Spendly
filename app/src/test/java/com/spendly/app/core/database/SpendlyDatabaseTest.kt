package com.spendly.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendly.app.data.local.BudgetEntity
import com.spendly.app.data.local.CategoryEntity
import com.spendly.app.data.local.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpendlyDatabaseTest {

    private lateinit var database: SpendlyDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SpendlyDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun transactionEntity(
        id: String,
        userId: String,
        amount: Double = 100.0,
        categoryId: String? = null
    ) = TransactionEntity(
        id = id,
        userId = userId,
        type = "EXPENSE",
        amount = amount,
        categoryId = categoryId,
        merchant = "Test",
        description = "",
        transactionDate = 0L,
        verificationStatus = "MANUAL_ENTRY",
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun `transaction dao only returns rows for the requested user`() = runTest {
        database.transactionDao().insert(transactionEntity("t1", userId = "userA"))
        database.transactionDao().insert(transactionEntity("t2", userId = "userA"))
        database.transactionDao().insert(transactionEntity("t3", userId = "userB"))

        val userATransactions = database.transactionDao().observeAll("userA").first()
        val userBTransactions = database.transactionDao().observeAll("userB").first()

        assertEquals(2, userATransactions.size)
        assertEquals(1, userBTransactions.size)
        assertEquals(true, userATransactions.all { it.userId == "userA" })
    }

    @Test
    fun `transaction persists across insert, update, and delete`() = runTest {
        val dao = database.transactionDao()
        dao.insert(transactionEntity("t1", userId = "userA", amount = 100.0))

        val inserted = dao.getById("t1")
        assertEquals(100.0, inserted?.amount)

        dao.update(inserted!!.copy(amount = 250.0))
        val updated = dao.getById("t1")
        assertEquals(250.0, updated?.amount)

        dao.deleteById("t1")
        assertNull(dao.getById("t1"))
    }

    @Test
    fun `category dao only returns rows for the requested user`() = runTest {
        val dao = database.categoryDao()
        dao.insertAll(
            listOf(
                CategoryEntity(id = "c1", userId = "userA", name = "Food", isDefault = true),
                CategoryEntity(id = "c2", userId = "userA", name = "Travel", isDefault = true),
                CategoryEntity(id = "c3", userId = "userB", name = "Food", isDefault = true)
            )
        )

        assertEquals(2, dao.countForUser("userA"))
        assertEquals(1, dao.countForUser("userB"))
        assertEquals(2, dao.observeAll("userA").first().size)
    }

    @Test
    fun `transaction category foreign key is cleared when category is deleted`() = runTest {
        database.categoryDao().insertAll(listOf(CategoryEntity(id = "c1", userId = "userA", name = "Food", isDefault = true)))
        database.transactionDao().insert(transactionEntity("t1", userId = "userA", categoryId = "c1"))

        database.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE id = 'c1'")

        val transaction = database.transactionDao().getById("t1")
        assertNull(transaction?.categoryId)
    }

    private fun budgetEntity(
        id: String,
        userId: String,
        categoryId: String? = null,
        period: String = "MONTHLY",
        amount: Double = 1000.0
    ) = BudgetEntity(
        id = id,
        userId = userId,
        categoryId = categoryId,
        amount = amount,
        priority = "MEDIUM",
        period = period,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun `budget dao only returns rows for the requested user`() = runTest {
        val dao = database.budgetDao()
        dao.insert(budgetEntity("b1", userId = "userA"))
        dao.insert(budgetEntity("b2", userId = "userA", period = "YEARLY"))
        dao.insert(budgetEntity("b3", userId = "userB"))

        assertEquals(2, dao.observeAll("userA").first().size)
        assertEquals(1, dao.observeAll("userB").first().size)
    }

    @Test
    fun `budget persists across insert, update, and delete`() = runTest {
        val dao = database.budgetDao()
        dao.insert(budgetEntity("b1", userId = "userA", amount = 1000.0))

        val inserted = dao.getById("b1")
        assertEquals(1000.0, inserted?.amount)

        dao.update(inserted!!.copy(amount = 2000.0))
        assertEquals(2000.0, dao.getById("b1")?.amount)

        dao.deleteById("b1")
        assertNull(dao.getById("b1"))
    }

    @Test
    fun `budget category foreign key is cleared when category is deleted`() = runTest {
        database.categoryDao().insertAll(listOf(CategoryEntity(id = "c1", userId = "userA", name = "Food", isDefault = true)))
        database.budgetDao().insert(budgetEntity("b1", userId = "userA", categoryId = "c1"))

        database.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE id = 'c1'")

        assertNull(database.budgetDao().getById("b1")?.categoryId)
    }

    @Test
    fun `findDuplicate detects same category and period, ignores other periods and self`() = runTest {
        database.categoryDao().insertAll(listOf(CategoryEntity(id = "c1", userId = "userA", name = "Food", isDefault = true)))
        val dao = database.budgetDao()
        dao.insert(budgetEntity("b1", userId = "userA", categoryId = "c1", period = "MONTHLY"))

        val duplicate = dao.findDuplicate("userA", "c1", "MONTHLY", excludeId = "")
        val differentPeriod = dao.findDuplicate("userA", "c1", "YEARLY", excludeId = "")
        val excludingSelf = dao.findDuplicate("userA", "c1", "MONTHLY", excludeId = "b1")

        assertEquals("b1", duplicate?.id)
        assertNull(differentPeriod)
        assertNull(excludingSelf)
    }
}

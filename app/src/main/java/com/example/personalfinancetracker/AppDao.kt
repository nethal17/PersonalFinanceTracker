package com.example.personalfinancetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE category = :category")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>
    
    @Insert
    suspend fun insertTransaction(transaction: Transaction)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>
    
    @Insert
    suspend fun insertCategory(category: Category)
    
    @Update
    suspend fun updateCategory(category: Category)
    
    @Delete
    suspend fun deleteCategory(category: Category)
    
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudget(month: Int, year: Int): Flow<Budget?>
    
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>
    
    @Insert
    suspend fun insertBudget(budget: Budget)
    
    @Update
    suspend fun updateBudget(budget: Budget)
    
    @Delete
    suspend fun deleteBudget(budget: Budget)
    
    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun validateUser(username: String, password: String): User?
    
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?
    
    @Insert
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
} 
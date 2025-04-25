package com.example.personalfinancetracker

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val database: AppDatabase) {
    // Transaction operations
    val allTransactions: Flow<List<Transaction>> = database.transactionDao().getAllTransactions()
    
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return database.transactionDao().getTransactionsByDateRange(startDate, endDate)
    }
    
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return database.transactionDao().getTransactionsByCategory(category)
    }
    
    suspend fun insertTransaction(transaction: Transaction) {
        database.transactionDao().insertTransaction(transaction)
    }
    
    suspend fun updateTransaction(transaction: Transaction) {
        database.transactionDao().updateTransaction(transaction)
    }
    
    suspend fun deleteTransaction(transaction: Transaction) {
        database.transactionDao().deleteTransaction(transaction)
    }
    
    suspend fun deleteAllTransactions() {
        database.transactionDao().deleteAllTransactions()
    }

    // Category operations
    val allCategories: Flow<List<Category>> = database.categoryDao().getAllCategories()
    
    suspend fun insertCategory(category: Category) {
        database.categoryDao().insertCategory(category)
    }
    
    suspend fun updateCategory(category: Category) {
        database.categoryDao().updateCategory(category)
    }
    
    suspend fun deleteCategory(category: Category) {
        database.categoryDao().deleteCategory(category)
    }
    
    suspend fun deleteAllCategories() {
        database.categoryDao().deleteAllCategories()
    }

    // Budget operations
    fun getBudget(month: Int, year: Int): Flow<Budget?> {
        return database.budgetDao().getBudget(month, year)
    }
    
    val allBudgets: Flow<List<Budget>> = database.budgetDao().getAllBudgets()
    
    suspend fun insertBudget(budget: Budget) {
        database.budgetDao().insertBudget(budget)
    }
    
    suspend fun updateBudget(budget: Budget) {
        database.budgetDao().updateBudget(budget)
    }
    
    suspend fun deleteBudget(budget: Budget) {
        database.budgetDao().deleteBudget(budget)
    }
    
    suspend fun deleteAllBudgets() {
        database.budgetDao().deleteAllBudgets()
    }

    // User operations
    suspend fun validateUser(username: String, password: String): User? {
        return database.userDao().validateUser(username, password)
    }
    
    suspend fun getUserByUsername(username: String): User? {
        return database.userDao().getUserByUsername(username)
    }
    
    suspend fun insertUser(user: User) {
        database.userDao().insertUser(user)
    }
    
    suspend fun updateUser(user: User) {
        database.userDao().updateUser(user)
    }
    
    suspend fun deleteUser(user: User) {
        database.userDao().deleteUser(user)
    }
    
    suspend fun deleteAllUsers() {
        database.userDao().deleteAllUsers()
    }
} 
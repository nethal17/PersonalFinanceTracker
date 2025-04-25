package com.example.personalfinancetracker

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataMigrationHelper(private val context: Context) {
    private val preferencesManager = PreferencesManager(context)
    private val userPreferences = UserPreferences(context)
    private val database = AppDatabase.getDatabase(context)
    private val repository = FinanceRepository(database)
    private val gson = Gson()

    suspend fun migrateData() = withContext(Dispatchers.IO) {
        try {
            // Migrate transactions
            val transactions = preferencesManager.getTransactions()
            transactions.forEach { transaction ->
                repository.insertTransaction(transaction)
            }

            // Migrate categories
            val categories = preferencesManager.getCategories()
            categories.forEach { category ->
                repository.insertCategory(category)
            }

            // Migrate budgets
            val allKeys = preferencesManager.getSharedPreferences().all.keys
            val budgetKeys = allKeys.filter { it.startsWith("budget_") }
            budgetKeys.forEach { key ->
                val budgetJson = preferencesManager.getSharedPreferences().getString(key, null)
                if (budgetJson != null) {
                    val budget = gson.fromJson(budgetJson, Budget::class.java)
                    repository.insertBudget(budget)
                }
            }

            // Migrate users
            val users = userPreferences.getUsers()
            users.forEach { user ->
                repository.insertUser(user)
            }

            // Clear SharedPreferences data after successful migration
            preferencesManager.getSharedPreferences().edit().clear().apply()
            userPreferences.getSharedPreferences().edit().clear().apply()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
} 
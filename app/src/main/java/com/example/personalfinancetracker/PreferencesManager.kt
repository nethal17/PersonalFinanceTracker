package com.example.personalfinancetracker

import android.graphics.Color
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

// PreferencesManager.kt
class PreferencesManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("finance_tracker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences
    }

    // Currency preference
    fun setCurrency(currency: String) {
        sharedPreferences.edit().putString("currency", currency).apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString("currency", "Rs.") ?: "Rs."
    }

    // Budget preferences
    fun setBudget(budget: Budget) {
        val budgetJson = gson.toJson(budget)
        val key = "budget_${budget.month}_${budget.year}"
        sharedPreferences.edit().putString(key, budgetJson).apply()
    }

    fun getBudget(month: Int, year: Int): Budget? {
        val key = "budget_${month}_${year}"
        val budgetJson = sharedPreferences.getString(key, null)
        return if (budgetJson != null) {
            gson.fromJson(budgetJson, Budget::class.java)
        } else {
            null
        }
    }

    // Transaction Preferences
    fun saveTransactions(transactions: List<Transaction>) {
        val transactionsJson = gson.toJson(transactions)
        sharedPreferences.edit().putString("transactions", transactionsJson).apply()
    }

    fun getTransactions(): List<Transaction> {
        val transactionsJson = sharedPreferences.getString("transactions", null)
        return if (transactionsJson != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(transactionsJson, type)
        } else {
            emptyList()
        }
    }

    // Categories Preferences
    fun saveCategories(categories: List<Category>) {
        val categoriesJson = gson.toJson(categories)
        sharedPreferences.edit().putString("categories", categoriesJson).apply()
    }

    fun getCategories(): List<Category> {
        val categoriesJson = sharedPreferences.getString("categories", null)
        return if (categoriesJson != null) {
            val type = object : TypeToken<List<Category>>() {}.type
            gson.fromJson(categoriesJson, type)
        } else {
            getDefaultCategories()
        }
    }

    /*
    fun getUsername(): String? {
        return sharedPreferences.getString("username", null)
    }

    fun setUsername(username: String) {
        sharedPreferences.edit().putString("username", username).apply()
    }

    fun getEmail(): String? {
        return sharedPreferences.getString("email", null)
    }

    fun setEmail(email: String) {
        sharedPreferences.edit().putString("email", email).apply()
    } */

    // App settings methods
    fun getLanguage(): String? {
        return sharedPreferences.getString("app_language", "English")
    }

    fun setLanguage(language: String) {
        sharedPreferences.edit().putString("app_language", language).apply()
    }

    fun getFirstDayOfWeek(): Int? {
        return sharedPreferences.getInt("first_day_of_week", Calendar.MONDAY)
    }

    fun setFirstDayOfWeek(day: Int) {
        sharedPreferences.edit().putInt("first_day_of_week", day).apply()
    }

    /* User session methods
    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    fun setUserLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    fun clearUserSession() {

        sharedPreferences.edit().putBoolean("is_logged_in", false).apply()
    } */

    private fun getDefaultCategories(): List<Category> {
        return listOf(
            Category(name = "Food", color = Color.rgb(255, 102, 102)), // Red
            Category(name = "Transport", color = Color.rgb(102, 178, 255)), // Blue
            Category(name = "Bills", color = Color.rgb(102, 255, 102)), // Green
            Category(name = "Entertainment", color = Color.rgb(255, 255, 102)), // Yellow
            Category(name = "Shopping", color = Color.rgb(255, 178, 102)), // Orange
            Category(name = "Health", color = Color.rgb(178, 102, 255)), // Purple
            Category(name = "Education", color = Color.rgb(208, 153, 0)), // Teal
            Category(name = "Other", color = Color.rgb(192, 192, 192)) // Gray
        )
    }

    fun clearAllBudgets() {

        val allKeys = sharedPreferences.all.keys
        val budgetKeys = allKeys.filter { it.startsWith("budget_") }
        val editor = sharedPreferences.edit()
        budgetKeys.forEach { editor.remove(it) }
        editor.apply()
    }
}
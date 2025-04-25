package com.example.personalfinancetracker

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val userPreferences = UserPreferences(context)
    private val gson = Gson()

    // Export data to internal storage
    fun exportData(): Boolean {
        try {
            // Get all data to backup
            val transactions = preferencesManager.getTransactions()
            val categories = preferencesManager.getCategories()
            val currency = preferencesManager.getCurrency()
            val currentUser = userPreferences.getCurrentUsername()
            val users = userPreferences.getUsers()

            // Get all budgets
            val allKeys = preferencesManager.getSharedPreferences().all.keys
            val budgetKeys = allKeys.filter { it.startsWith("budget_") }
            val budgets = budgetKeys.associate { key ->
                val budgetJson = preferencesManager.getSharedPreferences().getString(key, null)
                key to gson.fromJson(budgetJson, Budget::class.java)
            }

            // Create backup data object
            val backupData = BackupData(
                transactions = transactions,
                categories = categories,
                currency = currency,
                currentUser = currentUser,
                users = users,
                budgets = budgets
            )

            // Convert to JSON
            val jsonData = gson.toJson(backupData)

            // Create backup directory if it doesn't exist
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Create backup file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "finance_tracker_backup_$timestamp.json"
            val backupFile = File(backupDir, fileName)

            // Write to file
            FileOutputStream(backupFile).use { outputStream ->
                outputStream.write(jsonData.toByteArray())
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Import data from internal storage
    fun importData(fileName: String): Boolean {
        try {
            val backupFile = File(context.getExternalFilesDir(null), "backups/$fileName")
            if (!backupFile.exists()) {
                return false
            }

            // Read file
            val jsonData = backupFile.readText()

            // Parse JSON
            val type = object : TypeToken<BackupData>() {}.type
            val backupData: BackupData = gson.fromJson(jsonData, type)

            // Restore data
            preferencesManager.saveTransactions(backupData.transactions)
            preferencesManager.saveCategories(backupData.categories)
            preferencesManager.setCurrency(backupData.currency)
            
            // Restore budgets
            backupData.budgets.forEach { (key, budget) ->
                val budgetJson = gson.toJson(budget)
                preferencesManager.getSharedPreferences().edit().putString(key, budgetJson).apply()
            }
            
            // Restore user data
            userPreferences.saveUsers(backupData.users)
            if (backupData.currentUser.isNotEmpty()) {
                userPreferences.saveLoginState(true, backupData.currentUser)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Get list of available backup files
    fun getBackupFiles(): List<String> {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) {
            return emptyList()
        }
        
        return backupDir.listFiles()
            ?.filter { it.name.startsWith("finance_tracker_backup_") && it.name.endsWith(".json") }
            ?.map { it.name }
            ?.sortedByDescending { it }
            ?: emptyList()
    }

    fun getBackupFilePath(fileName: String): String {
        return File(context.getExternalFilesDir(null), "backups/$fileName").absolutePath
    }

    data class BackupData(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val currency: String,
        val currentUser: String,
        val users: List<User>,
        val budgets: Map<String, Budget>
    )
}
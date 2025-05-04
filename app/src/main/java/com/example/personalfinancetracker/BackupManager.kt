
package com.example.personalfinancetracker

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val userPreferences = UserPreferences(context)
    private val gson = Gson()
    private val database = AppDatabase.getDatabase(context)
    private val repository = FinanceRepository(database)

    suspend fun exportData(): Boolean {
        return try {
            Log.d("BackupManager", "Starting backup process...")

            // Collect all data first
            val transactions = repository.allTransactions.first()
            val categories = repository.allCategories.first()
            val budgets = repository.allBudgets.first()
            val users = userPreferences.getUsers()
            val currency = preferencesManager.getCurrency()
            val currentUser = userPreferences.getCurrentUsername()

            Log.d("BackupManager", "Collected data: " +
                    "${transactions.size} transactions, " +
                    "${categories.size} categories, " +
                    "${budgets.size} budgets, " +
                    "${users.size} users")

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
            Log.d("BackupManager", "Converted data to JSON")

            // Create backup directory
            val backupDir = File(context.getExternalFilesDir(null), "backups").apply {
                if (!exists()) {
                    Log.d("BackupManager", "Creating backup directory")
                    mkdirs()
                }
            }

            // Create backup file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "finance_tracker_backup_$timestamp.json"
            val backupFile = File(backupDir, fileName)
            Log.d("BackupManager", "Creating backup file at: ${backupFile.absolutePath}")

            // Write to file
            FileOutputStream(backupFile).use { outputStream ->
                outputStream.write(jsonData.toByteArray())
            }

            Log.d("BackupManager", "Backup completed successfully")
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Backup failed", e)
            false
        }
    }

    suspend fun importData(fileName: String): Boolean {
        return try {
            Log.d("BackupManager", "Starting restore process for file: $fileName")

            val backupDir = File(context.getExternalFilesDir(null), "backups")
            val backupFile = File(backupDir, fileName)

            if (!backupFile.exists()) {
                Log.e("BackupManager", "Backup file not found: ${backupFile.absolutePath}")
                return false
            }

            // Read and parse file
            val jsonData = backupFile.readText()
            val type = object : TypeToken<BackupData>() {}.type
            val backupData: BackupData = gson.fromJson(jsonData, type)

            // Clear existing data
            repository.deleteAllTransactions()
            repository.deleteAllCategories()
            repository.deleteAllBudgets()
            repository.deleteAllUsers()

            // Restore data
            backupData.transactions.forEach { repository.insertTransaction(it) }
            backupData.categories.forEach { repository.insertCategory(it) }
            backupData.budgets.forEach { repository.insertBudget(it) }
            backupData.users.forEach { repository.insertUser(it) }

            // Restore preferences
            preferencesManager.setCurrency(backupData.currency)
            if (backupData.currentUser.isNotEmpty()) {
                userPreferences.saveLoginState(true, backupData.currentUser)
            }

            // Verify some data was actually restored
            val restoredTransactions = repository.allTransactions.first().size
            val restoredCategories = repository.allCategories.first().size

            Log.d("BackupManager", "Restored $restoredTransactions transactions and $restoredCategories categories")

            // Consider it successful if we restored at least some data
            restoredTransactions > 0 || restoredCategories > 0
        } catch (e: Exception) {
            Log.e("BackupManager", "Restore failed", e)
            false
        }
    }
    fun getBackupFiles(): List<String> {
        return try {
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) {
                Log.d("BackupManager", "Backup directory doesn't exist yet")
                return emptyList()
            }

            val files = backupDir.listFiles()
            if (files == null) {
                Log.e("BackupManager", "Failed to list files in backup directory")
                return emptyList()
            }

            val backupFiles = files
                .filter { it.name.startsWith("finance_tracker_backup_") && it.name.endsWith(".json") }
                .map { it.name }
                .sortedByDescending { it }

            Log.d("BackupManager", "Found ${backupFiles.size} backup files")
            backupFiles
        } catch (e: Exception) {
            Log.e("BackupManager", "Error listing backup files", e)
            emptyList()
        }
    }

    fun getBackupFilePath(fileName: String): String {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        return File(backupDir, fileName).absolutePath
    }

    data class BackupData(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val currency: String,
        val currentUser: String,
        val users: List<User>,
        val budgets: List<Budget>
    )
}
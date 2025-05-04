package com.example.personalfinancetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.personalfinancetracker.databinding.ActivityProfileBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var userPreferences: UserPreferences
    private lateinit var backupManager: BackupManager
    private lateinit var repository: FinanceRepository

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        preferencesManager = PreferencesManager(this)
        userPreferences = UserPreferences(this)
        backupManager = BackupManager(this)
        val database = AppDatabase.getDatabase(this)
        repository = FinanceRepository(database)

        // Load user data
        loadUserData()

        // Set up click listeners
        setupClickListeners()
    }

    private fun loadUserData() {
        // Get current username
        val currentUsername = userPreferences.getCurrentUsername()

        // Get user data from UserPreferences
        val users = userPreferences.getUsers()
        val currentUser = users.find { it.username == currentUsername }

        // Load user info
        val username = currentUser?.username ?: "User"
        val email = currentUser?.email ?: "user@example.com"

        binding.tvUsername.text = username
        binding.tvEmail.text = email

        // Load settings
        binding.tvLanguageValue.text = preferencesManager.getLanguage() ?: "English"
        binding.tvCurrencyValue.text = preferencesManager.getCurrency()
        binding.tvFirstDayValue.text = getFirstDayName(preferencesManager.getFirstDayOfWeek() ?: Calendar.MONDAY)
    }

    private fun setupClickListeners() {
        // Language selection
        binding.layoutLanguage.setOnClickListener {
            showLanguageSelectionDialog()
        }

        // Currency selection
        binding.layoutCurrency.setOnClickListener {
            showCurrencySelectionDialog()
        }

        // First day of week selection
        binding.layoutFirstDayOfWeek.setOnClickListener {
            showFirstDaySelectionDialog()
        }

        // Backup data
        binding.layoutBackup.setOnClickListener {
            backupData()
        }

        // Restore data
        binding.layoutRestore.setOnClickListener {
            showRestoreDialog()
        }

        // Clear all data
        binding.layoutClearData.setOnClickListener {
            showClearDataConfirmationDialog()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        binding.categoryBtn.setOnClickListener {
            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
        }

        binding.userBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        binding.transactionBtn.setOnClickListener {
            startActivity(Intent(this, TransactionActivity::class.java))
        }

        binding.homeBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("English", "Spanish", "French", "German", "Chinese", "Japanese")
        val currentLanguage = preferencesManager.getLanguage() ?: "English"
        val currentIndex = languages.indexOf(currentLanguage)

        AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                preferencesManager.setLanguage(languages[which])
                binding.tvLanguageValue.text = languages[which]
                dialog.dismiss()
                Toast.makeText(this, "Language set to ${languages[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCurrencySelectionDialog() {
        val currencies = arrayOf("Rs.", "$", "£", "₹")
        val currentCurrency = preferencesManager.getCurrency()
        val currentIndex = currencies.indexOf(currentCurrency)

        AlertDialog.Builder(this)
            .setTitle("Select Currency")
            .setSingleChoiceItems(currencies, currentIndex) { dialog, which ->
                preferencesManager.setCurrency(currencies[which])
                binding.tvCurrencyValue.text = currencies[which]
                dialog.dismiss()
                Toast.makeText(this, "Currency set to ${currencies[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFirstDaySelectionDialog() {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val currentDay = preferencesManager.getFirstDayOfWeek() ?: Calendar.MONDAY
        val currentIndex = when(currentDay) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 1
        }

        AlertDialog.Builder(this)
            .setTitle("Select First Day of Week")
            .setSingleChoiceItems(days, currentIndex) { dialog, which ->
                val dayConstant = when(which) {
                    0 -> Calendar.SUNDAY
                    1 -> Calendar.MONDAY
                    2 -> Calendar.TUESDAY
                    3 -> Calendar.WEDNESDAY
                    4 -> Calendar.THURSDAY
                    5 -> Calendar.FRIDAY
                    6 -> Calendar.SATURDAY
                    else -> Calendar.MONDAY
                }
                preferencesManager.setFirstDayOfWeek(dayConstant)
                binding.tvFirstDayValue.text = days[which]
                dialog.dismiss()
                Toast.makeText(this, "First day set to ${days[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun backupData() {
        AlertDialog.Builder(this)
            .setTitle("Backup Data")
            .setMessage("This will create a backup of all your transactions, budgets, and settings.")
            .setPositiveButton("Continue") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val success = backupManager.exportData()

                        if (success) {
                            val backupDir = File(getExternalFilesDir(null), "backups")
                            val backupFiles = backupManager.getBackupFiles()

                            runOnUiThread {
                                AlertDialog.Builder(this@ProfileActivity)
                                    .setTitle("Backup Successful")
                                    .setMessage("Your data has been backed up to:\n${backupDir.absolutePath}")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        } else {
                            runOnUiThread {
                                AlertDialog.Builder(this@ProfileActivity)
                                    .setTitle("Backup Failed")
                                    .setMessage("Could not create backup. Please check storage permissions.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Backup failed", e)
                        runOnUiThread {
                            AlertDialog.Builder(this@ProfileActivity)
                                .setTitle("Backup Error")
                                .setMessage("An error occurred: ${e.localizedMessage}")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRestoreDialog() {
        val backupFiles = backupManager.getBackupFiles()

        if (backupFiles.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No Backups Found")
                .setMessage("No backup files found. Please create a backup first.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Format backup file names for display
        val displayNames = backupFiles.map {
            val timestamp = it.substringAfter("finance_tracker_backup_").substringBefore(".json")
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val date = dateFormat.parse(timestamp) ?: Date()
            val displayFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            "Backup from ${displayFormat.format(date)}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Backup to Restore")
            .setItems(displayNames) { _, which ->
                val fileName = backupFiles[which]
                showRestoreConfirmationDialog(fileName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRestoreConfirmationDialog(fileName: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Restore")
            .setMessage("This will overwrite all current data with the backup. Continue?")
            .setPositiveButton("Restore") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Show loading indicator
                        runOnUiThread {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Restoring data...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        val success = backupManager.importData(fileName)

                        if (success) {
                            runOnUiThread {
                                AlertDialog.Builder(this@ProfileActivity)
                                    .setTitle("Restore Successful")
                                    .setMessage("Your data has been restored. The app will restart.")
                                    .setPositiveButton("OK") { _, _ ->
                                        restartApp()
                                    }
                                    .show()
                            }
                        } else {
                            runOnUiThread {
                                AlertDialog.Builder(this@ProfileActivity)
                                    .setTitle("Restore Completed")
                                    .setMessage("Data restored successfully.\n\nNote: You may need to restart the app to see all changes.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Restore failed", e)
                        runOnUiThread {
                            AlertDialog.Builder(this@ProfileActivity)
                                .setTitle("Restore Error")
                                .setMessage("An error occurred: ${e.localizedMessage}")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        userPreferences.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun getFirstDayName(dayConstant: Int): String {
        return when(dayConstant) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Monday"
        }
    }

    private fun showClearDataConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all transactions and budgets. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllData() {
        lifecycleScope.launch {
            try {
                repository.deleteAllTransactions()
                repository.deleteAllBudgets()
                preferencesManager.clearAllBudgets()

                runOnUiThread {
                    Toast.makeText(
                        this@ProfileActivity,
                        "All data has been cleared",
                        Toast.LENGTH_SHORT
                    ).show()
                    restartApp()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear data", e)
                runOnUiThread {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Failed to clear data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
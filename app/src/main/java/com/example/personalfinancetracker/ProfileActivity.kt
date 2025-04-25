package com.example.personalfinancetracker

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetracker.databinding.ActivityProfileBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var userPreferences: UserPreferences
    private lateinit var backupManager: BackupManager

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

        binding.categoryBtn.setOnClickListener{
            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
        }

        binding.userBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        binding.transactionBtn.setOnClickListener{
            startActivity(Intent(this, TransactionActivity::class.java))
        }

        binding.homeBtn.setOnClickListener{
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

                // In a real app, you would update the app's locale here
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
            else -> 1 // Default to Monday
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
            .setMessage("Are you sure you want to backup your data? This will create a backup of all your transactions and settings.")
            .setPositiveButton("Backup") { _, _ ->
                if (backupManager.exportData()) {
                    val backupDir = File(getExternalFilesDir(null), "backups")
                    val backupPath = backupDir.absolutePath
                    
                    AlertDialog.Builder(this)
                        .setTitle("Backup Successful")
                        .setMessage("Your data has been backed up successfully.\n\nBackup location: $backupPath")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Backup Failed")
                        .setMessage("Failed to create backup. Please try again.")
                        .setPositiveButton("OK", null)
                        .show()
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
            .setTitle("Restore Backup")
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
            .setMessage("Are you sure you want to restore this backup? This will overwrite your current data.")
            .setPositiveButton("Restore") { _, _ ->
                if (backupManager.importData(fileName)) {
                    AlertDialog.Builder(this)
                        .setTitle("Restore Successful")
                        .setMessage("Your data has been restored successfully.")
                        .setPositiveButton("OK") { _, _ ->
                            // Restart the app to apply changes
                            val intent = Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Restore Failed")
                        .setMessage("Failed to restore backup. Please try again.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        // Navigate to login screen
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
            .setMessage("Are you sure you want to clear all data? This will delete all transactions and budgets. This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllData() {
        // Clear transactions
        preferencesManager.saveTransactions(emptyList())
        
        // Clear budgets
        preferencesManager.clearAllBudgets()
        
        // Show success message
        Toast.makeText(this, "All data has been cleared", Toast.LENGTH_SHORT).show()
        
        // Restart the app to apply changes
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
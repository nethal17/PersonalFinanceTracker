package com.example.personalfinancetracker

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.view.animation.DecelerateInterpolator
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: FinanceRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var backupManager: BackupManager
    private lateinit var transactions: MutableList<Transaction>
    private lateinit var adapter: TransactionAdapter
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Room database and repository
        val database = AppDatabase.getDatabase(this)
        repository = FinanceRepository(database)
        
        // Initialize other components
        notificationHelper = NotificationHelper(this)
        backupManager = BackupManager(this)
        preferencesManager = PreferencesManager(this)
        
        // Perform data migration if needed
        lifecycleScope.launch {
            val migrationHelper = DataMigrationHelper(this@MainActivity)
            migrationHelper.migrateData()
        }

        initializeComponents()
        setupUI()
        setupClickListeners()
        updateDashboard()
    }

    private fun initializeComponents() {
        transactions = mutableListOf()
        adapter = TransactionAdapter(transactions) { transaction ->
            // Handle transaction click
        }
        
        // Observe transactions from Room database
        lifecycleScope.launch {
            repository.allTransactions.collect { newTransactions ->
                transactions.clear()
                transactions.addAll(newTransactions)
                adapter.notifyDataSetChanged()
                updateDashboard()
            }
        }
    }

    private fun setupUI() {
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.transactionBtn.setOnClickListener {
            val intent = Intent(this, TransactionActivity::class.java)
            startActivity(intent)
        }

        binding.categoryBtn.setOnClickListener {
            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
        }

        binding.userBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.homeBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.btnSetBudget.setOnClickListener {
            showSetBudgetDialog()
        }
    }

    private fun updateDashboard() {
        // Update UI with data from Room database
        lifecycleScope.launch {
            val currentDate = Calendar.getInstance()
            val startOfMonth = getStartOfMonth(currentDate)
            val endOfMonth = getEndOfMonth(currentDate)
            
            repository.getTransactionsByDateRange(
                startOfMonth.timeInMillis,
                endOfMonth.timeInMillis
            ).collect { transactions ->
                val totalExpenses = transactions
                    .filter { it.isExpense }
                    .sumOf { it.amount }
                
                val totalIncome = transactions
                    .filter { !it.isExpense }
                    .sumOf { it.amount }
                
                // Update UI with new values
                binding.tvExpensesCard.text = formatAmount(getCurrency(), totalExpenses)
                binding.tvIncomeCard.text = formatAmount(getCurrency(), totalIncome)
                
                // Update budget status
                val (currentMonth, currentYear) = getCurrentMonthAndYear()
                checkAndUpdateBudgetStatus(currentMonth, currentYear, totalExpenses)
            }
        }
    }

    private fun getStartOfMonth(calendar: Calendar): Calendar {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        return start
    }

    private fun getEndOfMonth(calendar: Calendar): Calendar {
        val end = calendar.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)
        return end
    }

    private fun getCurrentMonthAndYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return Pair(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
    }

    private suspend fun checkAndUpdateBudgetStatus(month: Int, year: Int, expenses: Double) {
        repository.getBudget(month, year).collect { budget ->
            budget?.let {
                binding.tvBudgetCard.text = formatAmount(getCurrency(), it.amount)
                val budgetProgress = (expenses / it.amount * 100).toInt().coerceAtMost(100)
                
                // Animate progress bar
                binding.progressBudget.apply {
                    when {
                        budgetProgress >= 100 -> {
                            setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.progress_bar_red))
                        }
                        budgetProgress >= 80 -> {
                            setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.progress_bar_orange))
                        }
                        else -> {
                            setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.progress_bar_green))
                        }
                    }
                    
                    ObjectAnimator.ofInt(this, "progress", progress, budgetProgress).apply {
                        duration = 1000
                        interpolator = DecelerateInterpolator()
                        start()
                    }
                }

                when {
                    expenses >= it.amount -> {
                        showBudgetWarning("Budget exceeded!")
                        notificationHelper.showBudgetExceededNotification()
                    }
                    expenses >= it.amount * 0.8 -> {
                        showBudgetWarning("Approaching budget limit!")
                        notificationHelper.showApproachingBudgetNotification()
                    }
                    else -> hideBudgetWarning()
                }
            } ?: run {
                binding.tvBudgetCard.text = "No budget set"
                binding.progressBudget.progress = 0
                hideBudgetWarning()
            }
        }
    }

    private fun showBudgetWarning(message: String) {
        binding.tvBudgetWarning.apply {
            visibility = View.VISIBLE
            text = message
        }
    }

    private fun hideBudgetWarning() {
        binding.tvBudgetWarning.visibility = View.GONE
    }

    private fun showSetBudgetDialog() {
        lifecycleScope.launch {
            // Get current month and year
            val (currentMonth, currentYear) = getCurrentMonthAndYear()
            
            // Check if budget exists for current month
            val existingBudget = repository.getBudget(currentMonth, currentYear).firstOrNull()
            
            // Show dialog with existing budget amount if available
            SetBudgetDialog(
                this@MainActivity,
                existingBudget?.amount,
                onBudgetSet = { newBudget ->
                    lifecycleScope.launch {
                        if (existingBudget != null) {
                            // Update existing budget
                            val updatedBudget = existingBudget.copy(amount = newBudget.amount)
                            repository.updateBudget(updatedBudget)
                        } else {
                            // Insert new budget
                            repository.insertBudget(newBudget)
                        }
                        updateDashboard()
                    }
                }
            ).show()
        }
    }

    private fun formatAmount(currency: String, amount: Double): String {
        return "$currency ${amount.format(2)}"
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    private fun getCurrency(): String {
        return preferencesManager.getCurrency()
    }
}
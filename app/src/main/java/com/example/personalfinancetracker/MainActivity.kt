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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetracker.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.view.animation.DecelerateInterpolator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var backupManager: BackupManager
    private lateinit var transactions: MutableList<Transaction>
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupUI()
        setupClickListeners()
        updateDashboard()
    }

    private fun initializeComponents() {
        preferencesManager = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)
        backupManager = BackupManager(this)
        transactions = preferencesManager.getTransactions().toMutableList()
    }

    private fun setupUI() {

        val recentTransactions = transactions.sortedByDescending { it.date }.take(4)

        adapter = TransactionAdapter(recentTransactions) { transaction ->
            showEditTransactionDialog(transaction)
        }
        binding.rvTransactions.apply {
            adapter = this@MainActivity.adapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupClickListeners() {

        binding.categoryBtn.setOnClickListener{
            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
        }
        binding.userBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
        binding.btnSetBudget.setOnClickListener { showSetBudgetDialog() }

        binding.transactionBtn.setOnClickListener{
            startActivity(Intent(this, TransactionActivity::class.java))
        }
        binding.homeBtn.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun updateDashboard() {
        val (currentMonth, currentYear) = getCurrentMonthAndYear()
        val currentMonthTransactions = getTransactionsForMonth(currentMonth, currentYear)
        val (income, expenses) = calculateFinancials(currentMonthTransactions)

        updateFinancialViews(income, expenses)
        checkAndUpdateBudgetStatus(currentMonth, currentYear, expenses)
    }

    private fun getCurrentMonthAndYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return Pair(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
    }

    private fun getTransactionsForMonth(month: Int, year: Int): List<Transaction> {
        return transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }
    }

    private fun calculateFinancials(transactions: List<Transaction>): Triple<Double, Double, Double> {
        val income = transactions.filterNot { it.isExpense }.sumOf { it.amount }
        val expenses = transactions.filter { it.isExpense }.sumOf { it.amount }
        val balance = income - expenses
        return Triple(income, expenses, balance)
    }

    private fun updateFinancialViews(income: Double, expenses: Double) {
        val currency = preferencesManager.getCurrency()
        
        // Update card section
        binding.tvIncomeCard.text = formatAmount(currency, income)
        binding.tvExpensesCard.text = formatAmount(currency, expenses)
        
        // Add animation to cards
        animateCard(binding.incomeCard)
        animateCard(binding.expenseCard)
    }

    private fun animateCard(view: View) {
        ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 0.95f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.95f, 1f)
        ).apply {
            duration = 300
            start()
        }
    }

    private fun checkAndUpdateBudgetStatus(month: Int, year: Int, expenses: Double) {
        preferencesManager.getBudget(month, year)?.let { budget ->
            binding.tvBudgetCard.text = formatAmount(preferencesManager.getCurrency(), budget.amount)

            val budgetProgress = (expenses / budget.amount * 100).toInt().coerceAtMost(100)
            
            // Animate progress bar
            binding.progressBudget.apply {
                // Set vibrant colors based on progress
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
                
                // Animate progress with ObjectAnimator
                ObjectAnimator.ofInt(this, "progress", progress, budgetProgress).apply {
                    duration = 1000 // 1 second animation
                    interpolator = DecelerateInterpolator()
                    start()
                }
            }

            when {
                expenses >= budget.amount -> {
                    showBudgetWarning("Budget exceeded!")
                    notificationHelper.showBudgetExceededNotification()
                }
                expenses >= budget.amount * 0.8 -> {
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

    private fun showBudgetWarning(message: String) {
        binding.tvBudgetWarning.apply {
            visibility = View.VISIBLE
            text = message
        }
    }

    private fun hideBudgetWarning() {
        binding.tvBudgetWarning.visibility = View.GONE
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        EditTransactionDialog(this, transaction,
            onUpdate = { updatedTransaction ->
                val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
                if (index != -1) {
                    transactions[index] = updatedTransaction
                    preferencesManager.saveTransactions(transactions)
                    adapter.notifyItemChanged(index)
                    updateDashboard()
                }
            },
            onDelete = {
                val index = transactions.indexOfFirst { it.id == transaction.id }
                if (index != -1) {
                    transactions.removeAt(index)
                    preferencesManager.saveTransactions(transactions)
                    adapter.notifyItemRemoved(index)
                    updateDashboard()
                }
            }
        ).show()
    }

    private fun showSetBudgetDialog() {
        SetBudgetDialog(this) { budget ->
            preferencesManager.setBudget(budget)
            updateDashboard()
        }.show()
    }

    private fun formatAmount(currency: String, amount: Double): String {
        return "$currency ${amount.format(2)}"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        preferencesManager.saveTransactions(transactions)
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
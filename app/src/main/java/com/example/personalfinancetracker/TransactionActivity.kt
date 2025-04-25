package com.example.personalfinancetracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetracker.databinding.ActivityTransactionBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: FinanceRepository
    private lateinit var allTransactions: MutableList<Transaction>
    private lateinit var filteredTransactions: MutableList<Transaction>
    private lateinit var adapter: TransactionAdapter
    private var searchText: String = ""

    private enum class TransactionType {
        ALL, INCOME, EXPENSE
    }

    private var currentType = TransactionType.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Transactions"

        // Initialize Room database and repository
        val database = AppDatabase.getDatabase(this)
        repository = FinanceRepository(database)
        preferencesManager = PreferencesManager(this)

        initializeComponents()
        setupTabLayout()
        setupRecyclerView()
        setupClickListeners()
        setupSearchListener()
        observeTransactions()
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            repository.allTransactions.collect { transactions ->
                allTransactions = transactions.toMutableList()
                filterTransactions(currentType)
            }
        }
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchText = s.toString().trim()
                filterTransactions(currentType)
            }
        })
    }

    private fun initializeComponents() {
        allTransactions = mutableListOf()
        filteredTransactions = mutableListOf()
    }

    private fun setupTabLayout() {
        // Add tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Income"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Expenses"))

        // Set tab selection listener
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> filterTransactions(TransactionType.ALL)
                    1 -> filterTransactions(TransactionType.INCOME)
                    2 -> filterTransactions(TransactionType.EXPENSE)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(filteredTransactions) { transaction ->
            showEditTransactionDialog(transaction)
        }

        binding.rvTransactions.apply {
            this.adapter = this@TransactionActivity.adapter
            layoutManager = LinearLayoutManager(this@TransactionActivity)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }
        binding.categoryBtn.setOnClickListener{
            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
        }
        binding.transactionBtn.setOnClickListener{
            startActivity(Intent(this, TransactionActivity::class.java))
        }
        binding.homeBtn.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.userBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun filterTransactions(type: TransactionType) {
        currentType = type

        filteredTransactions.clear()

        // Apply filter based on selected type
        val typeFiltered = when (type) {
            TransactionType.ALL -> allTransactions
            TransactionType.INCOME -> allTransactions.filter { !it.isExpense }
            TransactionType.EXPENSE -> allTransactions.filter { it.isExpense }
        }

        // Apply search filter
        if (searchText.isNotEmpty()) {
            filteredTransactions.addAll(typeFiltered.filter { 
                it.title.contains(searchText, ignoreCase = true) 
            })
        } else {
            filteredTransactions.addAll(typeFiltered)
        }

        // Update adapter
        adapter.notifyDataSetChanged()

        // Show/hide empty state
        updateEmptyState()
    }

    private fun showAddTransactionDialog() {
        AddTransactionDialog(this) { transaction ->
            lifecycleScope.launch {
                repository.insertTransaction(transaction)
            }
        }.show()
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        EditTransactionDialog(
            this,
            transaction,
            onUpdate = { updatedTransaction ->
                lifecycleScope.launch {
                    repository.updateTransaction(updatedTransaction)
                }
            },
            onDelete = {
                lifecycleScope.launch {
                    repository.deleteTransaction(transaction)
                }
            }
        ).show()
    }

    private fun shouldIncludeTransaction(transaction: Transaction): Boolean {
        return when (currentType) {
            TransactionType.ALL -> true
            TransactionType.INCOME -> !transaction.isExpense
            TransactionType.EXPENSE -> transaction.isExpense
        }
    }

    private fun updateEmptyState() {
        if (filteredTransactions.isEmpty()) {
            binding.rvTransactions.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvTransactions.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
package com.example.personalfinancetracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetracker.databinding.ActivityTransactionBinding
import com.google.android.material.tabs.TabLayout

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    private lateinit var preferencesManager: PreferencesManager
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

        initializeComponents()
        setupTabLayout()
        setupRecyclerView()
        setupClickListeners()
        setupSearchListener()

        //filterTransactions(TransactionType.ALL)
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
        preferencesManager = PreferencesManager(this)
        allTransactions = preferencesManager.getTransactions().toMutableList()
        // Sort transactions by date (newest first)
        allTransactions.sortByDescending { it.date }
        filteredTransactions = allTransactions.toMutableList()
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
            // Add the new transaction to the main list
            allTransactions.add(0, transaction)
            preferencesManager.saveTransactions(allTransactions)

            // If the current filter includes this type of transaction, add it to filtered list
            if (shouldIncludeTransaction(transaction)) {
                filteredTransactions.add(0, transaction)
                adapter.notifyItemInserted(0)
                binding.rvTransactions.scrollToPosition(0)
            }

            // Update empty state visibility
            updateEmptyState()
        }.show()
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        EditTransactionDialog(
            this,
            transaction,
            onUpdate = { updatedTransaction ->
                // Find and update in the main list
                val mainIndex = allTransactions.indexOfFirst { it.id == updatedTransaction.id }
                if (mainIndex != -1) {
                    allTransactions[mainIndex] = updatedTransaction
                    preferencesManager.saveTransactions(allTransactions)
                }

                // Find and update in the filtered list if present
                val filteredIndex = filteredTransactions.indexOfFirst { it.id == updatedTransaction.id }
                if (filteredIndex != -1) {
                    // Check if the updated transaction still belongs in the current filter
                    if (shouldIncludeTransaction(updatedTransaction)) {
                        filteredTransactions[filteredIndex] = updatedTransaction
                        adapter.notifyItemChanged(filteredIndex)
                    } else {
                        // Remove if it no longer belongs in this filter
                        filteredTransactions.removeAt(filteredIndex)
                        adapter.notifyItemRemoved(filteredIndex)
                    }
                } else if (shouldIncludeTransaction(updatedTransaction)) {
                    // If it wasn't in the filtered list but now should be, add it
                    filteredTransactions.add(0, updatedTransaction)
                    adapter.notifyItemInserted(0)
                }

                // Update empty state visibility
                updateEmptyState()
            },
            onDelete = {
                // Find and remove from the main list
                val mainIndex = allTransactions.indexOfFirst { it.id == transaction.id }
                if (mainIndex != -1) {
                    allTransactions.removeAt(mainIndex)
                    preferencesManager.saveTransactions(allTransactions)
                }

                // Find and remove from the filtered list if present
                val filteredIndex = filteredTransactions.indexOfFirst { it.id == transaction.id }
                if (filteredIndex != -1) {
                    filteredTransactions.removeAt(filteredIndex)
                    adapter.notifyItemRemoved(filteredIndex)
                }

                // Update empty state visibility
                updateEmptyState()
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

    override fun onPause() {
        super.onPause()
        // Save transactions when activity is paused
        preferencesManager.saveTransactions(allTransactions)
    }
}
package com.example.personalfinancetracker

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import com.example.personalfinancetracker.databinding.DialogSetBudgetBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SetBudgetDialog(
    context: Context,
    private val onBudgetSet: (Budget) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogSetBudgetBinding
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSetBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(context)

        // Set up dialog
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Get current budget if exists
        val currentBudget = preferencesManager.getBudget(currentMonth, currentYear)
        if (currentBudget != null) {
            binding.etBudgetAmount.setText(currentBudget.amount.toString())
        }

        // Set month and year text
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = monthFormat.format(calendar.time)

        // Set currency
        val currency = preferencesManager.getCurrency()
        binding.tvCurrency.text = currency

        // Set up save button
        binding.btnSave.setOnClickListener {
            saveBudget(currentMonth, currentYear)
        }

        // Set up cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun saveBudget(month: Int, year: Int) {
        val amountStr = binding.etBudgetAmount.text.toString().trim()

        // Validate input
        if (amountStr.isEmpty()) {
            binding.etBudgetAmount.error = "Budget amount is required"
            return
        }

        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            binding.etBudgetAmount.error = "Invalid amount"
            return
        }

        if (amount <= 0) {
            binding.etBudgetAmount.error = "Amount must be greater than 0"
            return
        }

        // Create budget
        val budget = Budget(
            amount = amount,
            month = month,
            year = year
        )

        // Show success toast
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthYear = monthFormat.format(Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
            set(Calendar.YEAR, year)
        }.time)
        val currency = preferencesManager.getCurrency()
        Toast.makeText(
            context,
            "Budget set: $currency $amount for $monthYear",
            Toast.LENGTH_SHORT
        ).show()

        // Notify listener
        onBudgetSet(budget)

        // Dismiss dialog
        dismiss()
    }
}
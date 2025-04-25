package com.example.personalfinancetracker

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.personalfinancetracker.databinding.DialogEditTransactionBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.app.NotificationManager

class EditTransactionDialog(
    context: Context,
    private val transaction: Transaction,
    private val onUpdate: (Transaction) -> Unit,
    private val onDelete: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogEditTransactionBinding
    private lateinit var preferencesManager: PreferencesManager
    private var selectedDate = transaction.date
    private var isExpense = transaction.isExpense
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogEditTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(context)

        // Set up dialog dimensions
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Initialize UI with transaction data
        populateTransactionData()
        setupCategorySpinner()
        setupDatePicker()
        setupTransactionTypeToggle()
        setupActionButtons()
    }

    private fun populateTransactionData() {
        binding.etTitle.setText(transaction.title)
        binding.etAmount.setText(transaction.amount.toString())
        val currency = preferencesManager.getCurrency()
        binding.etAmount.hint = "Amount ($currency)"
        updateDateText()
    }

    private fun setupCategorySpinner() {
        val categories = preferencesManager.getCategories()
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Set selected category
        val position = categoryNames.indexOfFirst { it == transaction.category }
        if (position >= 0) {
            binding.spinnerCategory.setSelection(position)
        }
    }

    private fun setupDatePicker() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDate = calendar.timeInMillis
                updateDateText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = dateFormat.format(Date(selectedDate))
    }

    private fun setupTransactionTypeToggle() {
        binding.radioGroup.check(if (transaction.isExpense) R.id.radioExpense else R.id.radioIncome)
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            isExpense = checkedId == R.id.radioExpense
        }
    }

    private fun setupActionButtons() {
        binding.btnUpdate.setOnClickListener {
            if (validateInput()) {
                updateTransaction()
            }
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    // validate input
    private fun validateInput(): Boolean {
        var isValid = true

        if (binding.etTitle.text.toString().trim().isEmpty()) {
            binding.etTitle.error = "Title is required"
            isValid = false
        }

        val amountStr = binding.etAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Amount is required"
            isValid = false
        } else {
            try {
                if (amountStr.toDouble() <= 0) {
                    binding.etAmount.error = "Amount must be greater than 0"
                    isValid = false
                }
            } catch (e: NumberFormatException) {
                binding.etAmount.error = "Invalid amount"
                isValid = false
            }
        }

        return isValid
    }

    private fun updateTransaction() {
        val updatedTransaction = transaction.copy(
            title = binding.etTitle.text.toString().trim(),
            amount = binding.etAmount.text.toString().trim().toDouble(),
            category = binding.spinnerCategory.selectedItem as String,
            date = selectedDate,
            isExpense = isExpense
        )

        // Show success toast
        val currency = preferencesManager.getCurrency()
        val transactionType = if (isExpense) "Expense" else "Income"
        Toast.makeText(
            context,
            "$transactionType updated: ($currency ${updatedTransaction.amount}0)",
            Toast.LENGTH_SHORT
        ).show()

        onUpdate(updatedTransaction)
        dismiss()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(context)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                // Show delete toast
                val currency = preferencesManager.getCurrency()
                val transactionType = if (transaction.isExpense) "Expense" else "Income"
                Toast.makeText(
                    context,
                    "$transactionType deleted: $currency ${transaction.amount}0",
                    Toast.LENGTH_SHORT
                ).show()
                
                onDelete()
                dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
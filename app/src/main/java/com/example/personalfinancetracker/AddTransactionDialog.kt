package com.example.personalfinancetracker

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.personalfinancetracker.databinding.DialogAddTransactionBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionDialog(
    context: Context,
    private val onTransactionAdded: (Transaction) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogAddTransactionBinding
    private lateinit var preferencesManager: PreferencesManager
    private var selectedDate = Calendar.getInstance().timeInMillis
    private var isExpense = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(context)

        // Set up dialog
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Set currency hint
        val currency = preferencesManager.getCurrency()
        binding.etAmount.hint = "Amount ($currency)"

        // Set up category spinner
        val categories = preferencesManager.getCategories()
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Set up date picker
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        // Update date text
        updateDateText()

        // Set up transaction type radio buttons
        binding.radioExpense.isChecked = true
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            isExpense = checkedId == R.id.radioExpense
        }

        // Set up save button
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }

        // Set up cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        val datePickerDialog = DatePickerDialog(
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
        )

        datePickerDialog.show()
    }

    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = dateFormat.format(Date(selectedDate))
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem as String

        // Validate input
        if (title.isEmpty()) {
            binding.etTitle.error = "Title is required"
            return
        }

        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Amount is required"
            return
        }

        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            binding.etAmount.error = "Invalid amount"
            return
        }

        if (amount <= 0) {
            binding.etAmount.error = "Amount must be greater than 0"
            return
        }

        // Create transaction
        val transaction = Transaction(
            title = title,
            amount = amount,
            category = category,
            date = selectedDate,
            isExpense = isExpense
        )

        // Show success toast
        val currency = preferencesManager.getCurrency()
        val transactionType = if (isExpense) "Expense" else "Income"
        Toast.makeText(
            context,
            "New $transactionType added: ($currency ${amount}0)",
            Toast.LENGTH_SHORT
        ).show()

        // Notify listener
        onTransactionAdded(transaction)

        // Dismiss dialog
        dismiss()
    }
}
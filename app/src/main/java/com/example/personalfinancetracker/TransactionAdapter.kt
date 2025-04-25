package com.example.personalfinancetracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        preferencesManager = PreferencesManager(parent.context)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount() = transactions.size

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvTransactionCategory)
        private val tvDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivTransactionIcon)

        fun bind(transaction: Transaction) {
            tvTitle.text = transaction.title
            val currency = preferencesManager.getCurrency()
            tvAmount.text = "$currency ${transaction.amount.format(2)}"
            tvCategory.text = transaction.category

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(transaction.date))

            if (transaction.isExpense) {
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
                ivIcon.setImageResource(R.drawable.expense_arrow)
            } else {
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                ivIcon.setImageResource(R.drawable.income_arrow)
            }

            itemView.setOnClickListener { onItemClick(transaction) }
        }

        private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    }
}
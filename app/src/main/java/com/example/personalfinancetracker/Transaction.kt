package com.example.personalfinancetracker

data class Transaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String,
    var amount: Double,
    var category: String,
    var date: Long,
    var isExpense: Boolean = true
)
package com.example.personalfinancetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var amount: Double,
    var month: Int,
    var year: Int
)
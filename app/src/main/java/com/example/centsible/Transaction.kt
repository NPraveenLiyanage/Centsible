package com.example.centsible


data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,  // Format: YYYY-MM-DD
    val isIncome: Boolean
)

data class CategoryItem(
    val name: String,
    val emoji: String
)

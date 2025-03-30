package com.example.centsible

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.centsible.databinding.ActivityCategoryAnalysisBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CategoryAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryAnalysisBinding
    private var transactionList: MutableList<Transaction> = mutableListOf()
    private val sharedPref by lazy { getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE) }
    private val transactionsKey = "transactions"
    private val gson = Gson()
    private lateinit var adapter: CategoryAnalysisAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadTransactions()
        // Group transactions by category (only for expenses)
        val categoryTotals = transactionList
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        adapter = CategoryAnalysisAdapter(categoryTotals)
        binding.rvCategoryAnalysis.layoutManager = LinearLayoutManager(this)
        binding.rvCategoryAnalysis.adapter = adapter
    }

    private fun loadTransactions() {
        val json = sharedPref.getString(transactionsKey, null)
        transactionList = if(json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}
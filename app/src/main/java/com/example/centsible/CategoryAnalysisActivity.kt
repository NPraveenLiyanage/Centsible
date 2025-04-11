package com.example.centsible

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.centsible.adapters.CategoryAnalysisAdapter
import com.example.centsible.databinding.ActivityCategoryAnalysisBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.tabs.TabLayout
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

        // Setup TabLayout with two tabs.
        binding.tabs.addTab(binding.tabs.newTab().setText("Expense"))
        binding.tabs.addTab(binding.tabs.newTab().setText("Income"))

        // Set a listener to update analytics on tab changes.
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showAnalytics(isExpense = true)  // Expense analytics
                    1 -> showAnalytics(isExpense = false) // Income analytics
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) { }
            override fun onTabReselected(tab: TabLayout.Tab?) { }
        })

        // Default: Show Expense analytics.
        showAnalytics(isExpense = true)
    }

    private fun loadTransactions() {
        val json = sharedPref.getString(transactionsKey, null)
        transactionList = if (json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    /**
     * Updates the PieChart and the detailed RecyclerView.
     * @param isExpense If true, shows expense analytics; if false, shows income analytics.
     */
    private fun showAnalytics(isExpense: Boolean) {
        // Filter transactions based on type.
        val relevantTransactions = if (isExpense) {
            transactionList.filter { !it.isIncome }
        } else {
            transactionList.filter { it.isIncome }
        }
        // Group transactions by category and sum their amounts.
        val categoryTotals = relevantTransactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        setupPieChart(categoryTotals, isExpense)
        adapter = CategoryAnalysisAdapter(categoryTotals)
        binding.rvCategoryAnalysis.layoutManager = LinearLayoutManager(this)
        binding.rvCategoryAnalysis.adapter = adapter
    }

    private fun setupPieChart(categoryTotals: Map<String, Double>, isExpense: Boolean) {
        // Create PieEntry list.
        val entries = mutableListOf<PieEntry>()
        categoryTotals.forEach { (category, total) ->
            entries.add(PieEntry(total.toFloat(), category))
        }

        // Create the dataset and customize appearance.
        val dataSet = PieDataSet(entries, if (isExpense) "Expenses by Category" else "Income by Category")
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

        val pieData = PieData(dataSet)
        pieData.setValueTextSize(12f)
        pieData.setValueTextColor(Color.BLACK)

        with(binding.pieChart) {
            data = pieData
            description.isEnabled = false
            centerText = if(isExpense) "Expenses" else "Income"
            animateY(1000)
            setEntryLabelColor(Color.BLACK)
            invalidate() // Refresh chart view.
        }
    }
}
package com.example.centsible.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.centsible.R
import com.example.centsible.Transaction
import com.example.centsible.adapters.CategoryAnalysisAdapter
import com.example.centsible.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private var transactionList: MutableList<Transaction> = mutableListOf()
    private val sharedPref by lazy { requireContext().getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE) }
    private val transactionsKey = "transactions"
    private val gson = Gson()
    private lateinit var adapter: CategoryAnalysisAdapter
    private var customStartDate: Date? = null
    private var customEndDate: Date? = null

    companion object {
        fun newInstance() = AnalyticsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTransactions()

        binding.chipGroupPeriod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.chipCustom.id) {
                // When Custom is selected, show a dialog sequence to pick dates.
                showCustomDateRangeDialog()
            } else {
                // Reset any previously set custom dates.
                customStartDate = null
                customEndDate = null
                val isExpense = binding.tabs.selectedTabPosition == 0
                showAnalytics(isExpense)
            }
        }

        binding.tabs.addTab(binding.tabs.newTab().setText("Expense"))
        binding.tabs.addTab(binding.tabs.newTab().setText("Income"))

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isExpense = tab?.position == 0
                showAnalytics(isExpense)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Default: show Expense analytics.
        showAnalytics(isExpense = true)

        binding.btnViewAll.setOnClickListener {
            showViewAllDialog()
        }
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

    private fun getFilteredTransactions(transactions: List<Transaction>): List<Transaction> {
        val selectedChipId = binding.chipGroupPeriod.checkedChipId
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCal = Calendar.getInstance()
        return transactions.filter { transaction ->
            try {
                val transactionDate: Date = dateFormat.parse(transaction.date)
                val transCal = Calendar.getInstance().apply { time = transactionDate }
                when (selectedChipId) {
                    binding.chipWeek.id -> {
                        val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                        transactionDate.after(sevenDaysAgo.time) && !transactionDate.after(todayCal.time)
                    }
                    binding.chipMonth.id -> {
                        // In current month.
                        val currentMonth = todayCal.get(Calendar.MONTH)
                        val currentYear = todayCal.get(Calendar.YEAR)
                        transCal.get(Calendar.MONTH) == currentMonth && transCal.get(Calendar.YEAR) == currentYear
                    }
                    binding.chipYear.id -> {
                        // In current year.
                        val currentYear = todayCal.get(Calendar.YEAR)
                        transCal.get(Calendar.YEAR) == currentYear
                    }
                    binding.chipCustom.id -> {
                        if (customStartDate != null && customEndDate != null) {
                            !transactionDate.before(customStartDate) && !transactionDate.after(customEndDate)
                        } else {
                            false
                        }
                    }
                    else -> true
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun showAnalytics(isExpense: Boolean) {
        val filteredTransactions = getFilteredTransactions(transactionList)
        val relevantTransactions = if (isExpense) {
            filteredTransactions.filter { !it.isIncome }
        } else {
            filteredTransactions.filter { it.isIncome }
        }
        // Group by category and sum amounts.
        val categoryTotals = relevantTransactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        setupPieChart(categoryTotals, isExpense)
        adapter = CategoryAnalysisAdapter(categoryTotals)
        binding.rvCategoryAnalysis.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategoryAnalysis.adapter = adapter
    }

    private fun setupPieChart(categoryTotals: Map<String, Double>, isExpense: Boolean) {
        val entries = mutableListOf<PieEntry>()
        categoryTotals.forEach { (category, total) ->
            entries.add(PieEntry(total.toFloat(), category))
        }
        val dataSet = PieDataSet(entries, if (isExpense) "" else "")
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.setDrawValues(false)
        val pieData = PieData(dataSet)
        pieData.setValueTextColor(Color.BLACK)
        with(binding.pieChart) {
            data = pieData
            description.isEnabled = false
            centerText = if (isExpense) "Expenses" else "Income"
            legend.isEnabled = true
            legend.textColor = Color.WHITE
            animateY(1000)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(8f)
            setDrawEntryLabels(false)
            invalidate()
        }
    }

    private fun showViewAllDialog() {
        val filteredTransactions = getFilteredTransactions(transactionList)
        val isExpense = binding.tabs.selectedTabPosition == 0
        val relevantTransactions = if (isExpense) {
            filteredTransactions.filter { !it.isIncome }
        } else {
            filteredTransactions.filter { it.isIncome }
        }
        val categoryTotals = relevantTransactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_view_all_transactions)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val rvDialog = dialog.findViewById<RecyclerView>(R.id.rvDialogCategoryAnalysis)
        rvDialog.layoutManager = LinearLayoutManager(requireContext())

        val dialogAdapter = CategoryAnalysisAdapter(categoryTotals) { category ->
            dialog.dismiss()
            Toast.makeText(requireContext(), "Clicked on $category", Toast.LENGTH_SHORT).show()
        }
        rvDialog.adapter = dialogAdapter

        dialog.findViewById<MaterialButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        val titleText = if (isExpense) "Expense Categories" else "Income Categories"
        dialog.findViewById<TextView>(R.id.tvDialogTitle).text = titleText

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        dialog.show()
    }

    private fun showCustomDateRangeDialog() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startCal = Calendar.getInstance()

        // Prompt for Start Date.
        android.app.DatePickerDialog(
            requireContext(),
            { _, startYear, startMonth, startDay ->
                startCal.set(startYear, startMonth, startDay)
                customStartDate = startCal.time

                // prompt for End Date.
                val endCal = Calendar.getInstance()
                android.app.DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDay ->
                        endCal.set(endYear, endMonth, endDay)
                        customEndDate = endCal.time
                        if (customEndDate!!.before(customStartDate)) {
                            Toast.makeText(requireContext(), "End date must be after start date", Toast.LENGTH_SHORT).show()
                            customStartDate = null
                            customEndDate = null
                        } else {
                            binding.chipCustom.text = "${dateFormat.format(customStartDate)} to ${dateFormat.format(customEndDate)}"
                        }
                        // After picking custom dates (even if invalid), update analytics.
                        val isExpense = binding.tabs.selectedTabPosition == 0
                        showAnalytics(isExpense)
                    },
                    endCal.get(Calendar.YEAR),
                    endCal.get(Calendar.MONTH),
                    endCal.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            startCal.get(Calendar.YEAR),
            startCal.get(Calendar.MONTH),
            startCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
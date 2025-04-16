package com.example.centsible.fragments

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.centsible.NotificationHelper
import com.example.centsible.R
import com.example.centsible.Transaction
import com.example.centsible.adapters.CategoryAnalysisAdapter
import com.example.centsible.adapters.TransactionAdapter
import com.example.centsible.databinding.FragmentRecordsBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Make sure to import your custom budget update fragment:
import com.example.centsible.fragments.BudgetUpdateFragment

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    private val KEY_BUDGET_ALERT = "budget_alert_enabled"
    private var transactionList: MutableList<Transaction> = mutableListOf()
    private lateinit var transactionAdapter: TransactionAdapter
    private val gson = Gson()
    private lateinit var sharedPref: android.content.SharedPreferences

    private val budgetKey = "monthly_budget"
    private val transactionsKey = "transactions"
    private var monthlyBudget: Double = 0.0

    companion object {
        fun newInstance(): RecordsFragment = RecordsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        sharedPref = requireContext().getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check and request notification permission for Android 13+
        checkAndRequestNotificationPermission()

        // When the user taps the Monthly Budget TextView, navigate to the beautiful custom screen.
        binding.tvMonthlyBudget.setOnClickListener { showSetBudgetDialog() }

        // Configure RecyclerView.
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = TransactionAdapter(
            transactionList,
            onItemClick = { transaction ->
                // Open the bottom sheet for editing the transaction.
                val bottomSheet = com.example.centsible.TransactionDetailBottomSheet.newInstanceEdit(transaction, transaction.isIncome)
                bottomSheet.onTransactionSaved = {
                    // Refresh the list upon edit.
                    loadTransactions()
                    transactionAdapter.updateData(transactionList)
                    updateTransactionSummary()
                }
                bottomSheet.show(parentFragmentManager, "TransactionDetailBottomSheet")
            },
            onDeleteClick = { transaction ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Transaction")
                    .setMessage("Are you sure you want to delete this transaction?")
                    .setPositiveButton("Yes") { _, _ ->
                        deleteTransaction(transaction.id)
                        Toast.makeText(activity, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )
        binding.rvTransactions.adapter = transactionAdapter

        // Load initial data.
        loadMonthlyBudget()
        loadTransactions()
        updateBudgetSummary()
        updateTransactionSummary()

        // VIEW ALL BUTTON: When clicked, launch a dialog showing category analysis.
        binding.btnViewAll.setOnClickListener {
            val categoryTotals: MutableMap<String, Double> = mutableMapOf()
            transactionList.forEach { transaction ->
                val category = transaction.category
                val total = categoryTotals[category] ?: 0.0
                categoryTotals[category] = total + transaction.amount
            }
            val dialogView = layoutInflater.inflate(R.layout.dialog_view_all_transactions, null)
            val rvDialog = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDialogCategoryAnalysis)
            rvDialog.layoutManager = LinearLayoutManager(requireContext())
            val adapter = CategoryAnalysisAdapter(categoryTotals) { category ->
                Toast.makeText(requireContext(), "Clicked on $category", Toast.LENGTH_SHORT).show()
            }
            rvDialog.adapter = adapter
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Category Analysis")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    private fun showSetBudgetDialog() {
        // Create a Dialog using the custom bottom sheet style
        val dialog = Dialog(requireContext(), R.style.BottomSheetDialogTheme)
        dialog.setContentView(R.layout.fragment_budget_update)

        // Make the dialog width match the screen width and height wrap content,
        // and position it at the bottom of the screen.
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            // Optionally, add window animations:
            // attributes.windowAnimations = R.style.BottomSheetAnimation
        }

        // Get shared preferences (same as you use elsewhere)
        val sharedPref = requireContext().getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)

        // Pre-fill the budget EditText if a current budget exists
        val etBudget = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBudget)
        val currentBudget = sharedPref.getFloat(budgetKey, 0.0f)
        if (currentBudget > 0f) {
            etBudget.setText(currentBudget.toString())
        }

        // Set up the Save button in your custom layout
        val btnSaveBudget = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveBudget)
        btnSaveBudget.setOnClickListener {
            val budgetText = etBudget.text.toString()
            if (budgetText.isNotEmpty()) {
                val newBudget = budgetText.toFloatOrNull() ?: 0f
                sharedPref.edit().putFloat(budgetKey, newBudget).apply()
                updateBudgetSummary()  // Update UI accordingly
                Toast.makeText(requireContext(), "Monthly budget updated", Toast.LENGTH_SHORT).show()
                dialog.dismiss() // Close the bottom sheet
            } else {
                Toast.makeText(requireContext(), "Please enter a valid budget", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    // Request POST_NOTIFICATIONS permission for Android 13+.
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadMonthlyBudget()
        loadTransactions()
        transactionAdapter.updateData(transactionList)
        updateBudgetSummary()
        updateTransactionSummary()
    }

    private fun loadMonthlyBudget() {
        monthlyBudget = sharedPref.getFloat(budgetKey, 0.0f).toDouble()
    }

    // Load saved transactions from SharedPreferences.
    private fun loadTransactions() {
        val json = sharedPref.getString(transactionsKey, null)
        transactionList = if (json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    private fun updateBudgetSummary() {
        binding.tvMonthlyBudget.text = "Monthly Budget: LKR$monthlyBudget"
        val totalExpense = transactionList.filter { !it.isIncome }.sumOf { it.amount }
        if (monthlyBudget > 0) {
            val status = when {
                totalExpense >= monthlyBudget -> "Exceeded Budget!"
                totalExpense >= 0.9 * monthlyBudget -> "Near Budget Limit"
                else -> "Within Budget"
            }
            binding.tvBudgetStatus.text = status
            val budgetAlertsEnabled = sharedPref.getBoolean(KEY_BUDGET_ALERT, true)
            if (status != "Within Budget" && budgetAlertsEnabled) {
                Toast.makeText(
                    requireContext(),
                    "Budget Alert: You have $status. Total expenses: $$totalExpense",
                    Toast.LENGTH_LONG
                ).show()
                NotificationHelper(requireContext()).sendBudgetAlertNotification(
                    "Budget Alert",
                    "You have $status. Total expenses: $$totalExpense"
                )
            }
        } else {
            binding.tvBudgetStatus.text = "Set your monthly budget"
        }
    }

    private fun updateTransactionSummary() {
        val totalIncome = transactionList.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = transactionList.filter { !it.isIncome }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        binding.tvTransactionSummary.text = """
                    💰 Income:   LKR${String.format("%.2f", totalIncome)}
                    💸 Expenses: LKR${String.format("%.2f", totalExpense)}
                    ⚖️ Balance:  LKR${String.format("%.2f", netBalance)}
                """.trimIndent()
    }

    private fun deleteTransaction(id: String) {
        transactionList.removeAll { it.id == id }
        with(sharedPref.edit()) {
            putString(transactionsKey, gson.toJson(transactionList))
            apply()
        }
        transactionAdapter.updateData(transactionList)
        updateBudgetSummary()
        updateTransactionSummary()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
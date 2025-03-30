package com.example.centsible

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.centsible.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactionList: MutableList<Transaction> = mutableListOf()
    private val sharedPref by lazy { getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE) }
    private val gson = Gson()
    private var monthlyBudget: Double = 0.0
    private val budgetKey = "monthly_budget"
    private val transactionsKey = "transactions"

    // Launcher to request POST_NOTIFICATIONS permission (for Android 13+)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission if on Android 13 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Allow user to tap the monthly budget text to update the budget.
        binding.tvMonthlyBudget.setOnClickListener {
            showSetBudgetDialog()
        }

        // Load saved monthly budget and transactions.
        loadMonthlyBudget()
        loadTransactions()

        // Setup RecyclerView.
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(
            transactionList,
            onItemClick = { transaction ->
                // Open the add/edit activity for editing.
                val intent = Intent(this, AddTransactionActivity::class.java)
                intent.putExtra("edit", true)
                intent.putExtra("transactionId", transaction.id)
                startActivity(intent)
            },
            onDeleteClick = { transaction ->
                // When tapping the delete button, confirm deletion.
                AlertDialog.Builder(this)
                    .setTitle("Delete Transaction")
                    .setMessage("Are you sure you want to delete this transaction?")
                    .setPositiveButton("Yes") { _, _ ->
                        deleteTransaction(transaction.id)
                        Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )
        binding.rvTransactions.adapter = transactionAdapter

        // FAB: Open the add transaction activity.
        binding.fabAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        // Button: Open Category Analysis
        binding.btnCategoryAnalysis.setOnClickListener {
            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
        }

        // Button: Open Backup & Restore screen.
        binding.btnBackupRestore.setOnClickListener {
            startActivity(Intent(this, BackupRestoreActivity::class.java))
        }

        updateBudgetSummary()
    }

    override fun onResume() {
        super.onResume()
        // Reload budget and transaction data when resuming.
        loadMonthlyBudget()
        loadTransactions()
        transactionAdapter.updateData(transactionList)
        updateBudgetSummary()
    }

    private fun loadMonthlyBudget() {
        monthlyBudget = sharedPref.getFloat(budgetKey, 0.0f).toDouble()
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

    fun saveTransactions() {
        val editor = sharedPref.edit()
        val json = gson.toJson(transactionList)
        editor.putString(transactionsKey, json)
        editor.apply()
    }

    fun deleteTransaction(id: String) {
        transactionList.removeAll { it.id == id }
        saveTransactions()
        transactionAdapter.updateData(transactionList)
        updateBudgetSummary()
    }

    private fun updateBudgetSummary() {
        // Indicate that the budget can be tapped to update.
        binding.tvMonthlyBudget.text = "Monthly Budget: $$monthlyBudget (Tap to update)"
        // Calculate total expenses (for expense transactions only).
        val totalExpense = transactionList.filter { !it.isIncome }
            .sumOf { it.amount }
        if (monthlyBudget > 0) {
            val status = when {
                totalExpense >= monthlyBudget -> "Exceeded Budget!"
                totalExpense >= 0.9 * monthlyBudget -> "Near Budget Limit"
                else -> "Within Budget"
            }
            binding.tvBudgetStatus.text = "Status: $status"
            if (status != "Within Budget") {
                // Send a system notification via NotificationHelper.
                NotificationHelper(this).sendBudgetAlertNotification(
                    "Budget Alert",
                    "You have $status. Total expenses: $$totalExpense"
                )
                // Also show a Toast message.
                Toast.makeText(
                    this,
                    "Budget Alert: You have $status. Total expenses: $$totalExpense",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            binding.tvBudgetStatus.text = "Set your monthly budget"
        }
    }

    // Opens a dialog that allows the user to set or update the monthly budget.
    private fun showSetBudgetDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Monthly Budget")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)
        builder.setPositiveButton("Set Budget") { _, _ ->
            val budgetText = input.text.toString()
            if (budgetText.isNotEmpty()) {
                monthlyBudget = budgetText.toDoubleOrNull() ?: 0.0
                val editor = sharedPref.edit()
                editor.putFloat(budgetKey, monthlyBudget.toFloat())
                editor.apply()
                updateBudgetSummary()
                Toast.makeText(this, "Monthly budget updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid Budget", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
}
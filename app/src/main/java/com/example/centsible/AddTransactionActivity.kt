package com.example.centsible

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.centsible.databinding.ActivityAddTransactionBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private val sharedPref by lazy { getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE) }
    private val gson = Gson()
    private val transactionsKey = "transactions"
    private var isEditMode = false
    private var editingTransactionId: String? = null
    private var transactionList: MutableList<Transaction> = mutableListOf()

    // Sample categories – "Salary" is considered income; others represent expense categories.
    private val categories = arrayOf("Food", "Transport", "Bills", "Entertainment", "Salary", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Determine if we are in edit mode.
        isEditMode = intent.getBooleanExtra("edit", false)
        editingTransactionId = intent.getStringExtra("transactionId")
        loadTransactions()

        // Setup the category spinner.
        binding.spinnerCategory.adapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, categories
        )

        // If editing an existing transaction, populate the fields with its data.
        if (isEditMode && editingTransactionId != null) {
            val transaction = transactionList.find { it.id == editingTransactionId }
            transaction?.let {
                binding.etTitle.setText(it.title)
                binding.etAmount.setText(it.amount.toString())
                val index = categories.indexOf(it.category)
                if (index >= 0) binding.spinnerCategory.setSelection(index)
                binding.etDate.setText(it.date)
            }
        }

        // Set up the date field to open a DatePicker when tapped.
        binding.etDate.inputType = InputType.TYPE_NULL
        binding.etDate.setOnClickListener { showDatePicker() }

        // Handle the Save button.
        binding.btnSave.setOnClickListener { saveTransaction() }
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

    private fun saveTransactions() {
        val editor = sharedPref.edit()
        val json = gson.toJson(transactionList)
        editor.putString(transactionsKey, json)
        editor.apply()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Format the date as YYYY-MM-DD.
                val formattedMonth = month + 1 // Month is zero-indexed.
                val formattedDate = String.format("%04d-%02d-%02d", year, formattedMonth, dayOfMonth)
                binding.etDate.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem?.toString() ?: ""

        // Validate the Title.
        if (title.isEmpty()) {
            binding.etTitle.error = "Title is required"
            binding.etTitle.requestFocus()
            return
        }

        // Validate the Amount.
        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Amount is required"
            binding.etAmount.requestFocus()
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: run {
            binding.etAmount.error = "Enter a valid number"
            binding.etAmount.requestFocus()
            return
        }

        // Validate the Date.
        if (date.isEmpty()) {
            binding.etDate.error = "Date is required"
            binding.etDate.requestFocus()
            return
        }

        // Determine if the transaction is income (assuming "Salary" means income).
        val isIncome = category == "Salary"

        // Update or add the transaction.
        if (isEditMode && editingTransactionId != null) {
            val index = transactionList.indexOfFirst { it.id == editingTransactionId }
            if (index != -1) {
                transactionList[index] = Transaction(editingTransactionId!!, title, amount, category, date, isIncome)
            }
        } else {
            val newTransaction = Transaction(UUID.randomUUID().toString(), title, amount, category, date, isIncome)
            transactionList.add(newTransaction)
        }

        saveTransactions()
        Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show()
        finish()  // Return to MainActivity.
    }
}
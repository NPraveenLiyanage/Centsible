package com.example.centsible

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.centsible.databinding.TransactionDetailBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class TransactionDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: TransactionDetailBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var selectedCategory: CategoryItem? = null
    private var isIncomeSelected: Boolean = false
    private var isEdit: Boolean = false
    private var transactionId: String? = null

    private var transactionList: MutableList<Transaction> = mutableListOf()
    private lateinit var sharedPref: android.content.SharedPreferences
    private val transactionsKey = "transactions"
    private val gson = Gson()

    // Listener to notify that a transaction is saved.
    var onTransactionSaved: (() -> Unit)? = null

    companion object {
        private const val ARG_TRANSACTION_ID = "arg_transaction_id"
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_IS_INCOME = "arg_is_income"
        private const val ARG_IS_EDIT = "arg_is_edit"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_AMOUNT = "arg_amount"
        private const val ARG_DATE = "arg_date"

        // For new transactions.
        fun newInstance(category: CategoryItem, isIncome: Boolean): TransactionDetailBottomSheet {
            val args = Bundle().apply {
                putString(ARG_CATEGORY, category.name) // Only sending the name; emoji is for display
                putBoolean(ARG_IS_INCOME, isIncome)
                putBoolean(ARG_IS_EDIT, false)
            }
            return TransactionDetailBottomSheet().apply { arguments = args }
        }

        // For editing an existing transaction.
        fun newInstanceEdit(transaction: Transaction, isIncome: Boolean): TransactionDetailBottomSheet {
            val args = Bundle().apply {
                putString(ARG_TRANSACTION_ID, transaction.id)
                putString(ARG_CATEGORY, transaction.category)
                putString(ARG_TITLE, transaction.title)
                putDouble(ARG_AMOUNT, transaction.amount)
                putString(ARG_DATE, transaction.date)
                putBoolean(ARG_IS_INCOME, isIncome)
                putBoolean(ARG_IS_EDIT, true)
            }
            return TransactionDetailBottomSheet().apply { arguments = args }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sharedPref = context.getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)
        // Load existing transactions.
        val json = sharedPref.getString(transactionsKey, null)
        transactionList = if (json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = TransactionDetailBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Retrieve passed parameters.
        isIncomeSelected = arguments?.getBoolean(ARG_IS_INCOME) ?: false
        isEdit = arguments?.getBoolean(ARG_IS_EDIT, false) ?: false

        if (isEdit) {
            // Edit mode: prepopulate fields.
            transactionId = arguments?.getString(ARG_TRANSACTION_ID)
            binding.etTitleBS.setText(arguments?.getString(ARG_TITLE, ""))
            binding.etAmountBS.setText(arguments?.getDouble(ARG_AMOUNT, 0.0).toString())
            binding.etDateBS.setText(arguments?.getString(ARG_DATE, ""))
            val categoryName = arguments?.getString(ARG_CATEGORY, "Category")
            binding.tvSelectedCategoryBS.text = categoryName
            selectedCategory = CategoryItem(categoryName ?: "Category", "")
            // Update button text to indicate editing.
            binding.btnSaveBS.text = "Update Transaction"
        } else {
            // New transaction mode.
            val categoryName = arguments?.getString(ARG_CATEGORY, "Category")
            binding.tvSelectedCategoryBS.text = categoryName
            selectedCategory = CategoryItem(categoryName ?: "Category", "")
        }

        // Setup date field.
        binding.etDateBS.inputType = InputType.TYPE_NULL
        binding.etDateBS.setOnClickListener { showDatePicker() }

        binding.btnSaveBS.setOnClickListener { saveTransaction() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formattedMonth = month + 1
                binding.etDateBS.setText(String.format("%04d-%02d-%02d", year, formattedMonth, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun saveTransaction() {
        val title = binding.etTitleBS.text.toString().trim()
        val amountStr = binding.etAmountBS.text.toString().trim()
        val date = binding.etDateBS.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitleBS.error = "Title is required"
            binding.etTitleBS.requestFocus()
            return
        }
        if (amountStr.isEmpty()) {
            binding.etAmountBS.error = "Amount is required"
            binding.etAmountBS.requestFocus()
            return
        }
        val amount = amountStr.toDoubleOrNull() ?: run {
            binding.etAmountBS.error = "Enter a valid number"
            binding.etAmountBS.requestFocus()
            return
        }
        if (date.isEmpty()) {
            binding.etDateBS.error = "Date is required"
            binding.etDateBS.requestFocus()
            return
        }

        if (isEdit) {
            // Edit mode: update existing transaction.
            val id = transactionId ?: UUID.randomUUID().toString()
            val updatedTransaction = Transaction(
                id = id,
                title = title,
                amount = amount,
                category = selectedCategory?.name ?: "Category",
                date = date,
                isIncome = isIncomeSelected
            )
            val index = transactionList.indexOfFirst { it.id == id }
            if (index != -1) {
                transactionList[index] = updatedTransaction
            } else {
                transactionList.add(updatedTransaction)
            }
            Toast.makeText(requireContext(), "Transaction updated", Toast.LENGTH_SHORT).show()
        } else {
            // New transaction: add it.
            val newTransaction = Transaction(
                id = UUID.randomUUID().toString(),
                title = title,
                amount = amount,
                category = selectedCategory?.name ?: "Category",
                date = date,
                isIncome = isIncomeSelected
            )
            transactionList.add(newTransaction)
            Toast.makeText(requireContext(), "Transaction saved", Toast.LENGTH_SHORT).show()
        }

        // Save the updated list into SharedPreferences.
        val editor = sharedPref.edit()
        editor.putString(transactionsKey, gson.toJson(transactionList))
        editor.apply()

        // Invoke callback to refresh data without closing the host activity.
        onTransactionSaved?.invoke()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
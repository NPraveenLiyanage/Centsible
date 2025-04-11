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
    private var transactionList: MutableList<Transaction> = mutableListOf()
    private lateinit var sharedPref: android.content.SharedPreferences
    private val transactionsKey = "transactions"
    private val gson = Gson()

    // Listener to notify the activity when a new transaction is saved.
    var onTransactionSaved: (() -> Unit)? = null

    companion object {
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_IS_INCOME = "arg_is_income"

        fun newInstance(category: CategoryItem, isIncome: Boolean): TransactionDetailBottomSheet {
            val args = Bundle().apply {
                putString(ARG_CATEGORY, category.name) // we send just the name; emoji is for display
                putBoolean(ARG_IS_INCOME, isIncome)
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
        val categoryName = arguments?.getString(ARG_CATEGORY, "Category")
        isIncomeSelected = arguments?.getBoolean(ARG_IS_INCOME) ?: false
        selectedCategory = CategoryItem(categoryName ?: "Category", "") // Emoji can be looked up if needed.

        binding.tvSelectedCategoryBS.text = categoryName

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

        // Create and add the new transaction.
        val newTransaction = Transaction(
            UUID.randomUUID().toString(),
            title,
            amount,
            selectedCategory?.name ?: "Category",
            date,
            isIncomeSelected
        )
        transactionList.add(newTransaction)
        // Save to SharedPreferences.
        val editor = sharedPref.edit()
        editor.putString(transactionsKey, gson.toJson(transactionList))
        editor.apply()

        Toast.makeText(requireContext(), "Transaction saved", Toast.LENGTH_SHORT).show()
        onTransactionSaved?.invoke()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
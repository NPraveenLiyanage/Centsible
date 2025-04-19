package com.example.centsible.fragments

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.centsible.CategoryItem
import com.example.centsible.R
import com.example.centsible.Transaction
import com.example.centsible.TransactionDetailBottomSheet
import com.example.centsible.databinding.FragmentAddTransactionBinding
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val sharedPref by lazy {
        requireContext().getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)
    }
    private val gson = Gson()
    private var transactionList: MutableList<Transaction> = mutableListOf()

    // Combined Expense Categories
    private val expenseCategories = listOf(
        CategoryItem("Food", "🍔"),
        CategoryItem("Transport", "🚞"),
        CategoryItem("Bills", "💶"),
        CategoryItem("Entertainment", "🎬"),
        CategoryItem("Shopping", "🛍️"),
        CategoryItem("Health", "🏥"),
        CategoryItem("Travel", "✈️"),
        CategoryItem("Utilities", "💡"),
        CategoryItem("Education", "🎓"),
        CategoryItem("Phone", "📱"),
        CategoryItem("Beauty", "💄"),
        CategoryItem("Sports", "⚽"),
        CategoryItem("Social", "👥"),
        CategoryItem("Clothing", "👗"),
        CategoryItem("Car", "🚗"),
        CategoryItem("Alcohol", "🍺"),
        CategoryItem("Electronics", "📺"),
        CategoryItem("Pets", "🐶"),
        CategoryItem("Repair", "🔧"),
        CategoryItem("Housing", "🏠"),
        CategoryItem("Home", "🏡"),
        CategoryItem("Gift", "🎁"),
        CategoryItem("Donation", "🤝"),
        CategoryItem("Kids", "👶"),
        CategoryItem("Other Expense", "💸")
    )

    // Income Categories.
    private val incomeCategories = listOf(
        CategoryItem("Salary", "💰"),
        CategoryItem("Business", "🏢"),
        CategoryItem("Investments", "💵"),
        CategoryItem("Freelance", "💻"),
        CategoryItem("Rental Income", "🏠"),
        CategoryItem("Interest", "💲"),
        CategoryItem("Dividends", "💳"),
        CategoryItem("Other Income", "🎁")
    )

    private var isIncomeSelected: Boolean = false

    companion object {
        fun newInstance(): AddTransactionFragment = AddTransactionFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if this fragment is launched in "edit" mode.
        val isEditMode = requireActivity().intent.getBooleanExtra("edit", false)
        if (isEditMode) {
            loadTransactions()
            val transactionId = requireActivity().intent.getStringExtra("transactionId")
            val transactionToEdit = transactionList.find { it.id == transactionId }
            if (transactionToEdit != null) {
                isIncomeSelected = transactionToEdit.isIncome
                // Launch the bottom sheet for editing.
                val bottomSheet = TransactionDetailBottomSheet.newInstanceEdit(transactionToEdit, isIncomeSelected)
                bottomSheet.onTransactionSaved = {
                    Toast.makeText(requireContext(), "Transaction updated", Toast.LENGTH_SHORT).show()
                }
                bottomSheet.show(parentFragmentManager, "TransactionDetailBottomSheet")
                return  // Skip the category selection UI.
            } else {
                Toast.makeText(requireContext(), "Transaction not found", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
                return
            }
        }

        // Normal "add new transaction" flow.
        binding.formLayout.visibility = View.GONE

        // Configure TabLayout with two tabs: Expense and Income.
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Expense"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Income"))

        // Initially load expense categories into the RecyclerView.
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 3)
        loadCategories(expenseCategories)
        isIncomeSelected = false

        // Listen for tab changes.
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    when (it.position) {
                        0 -> {
                            loadCategories(expenseCategories)
                            isIncomeSelected = false
                        }
                        1 -> {
                            loadCategories(incomeCategories)
                            isIncomeSelected = true
                        }
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // Load category list into the RecyclerView.
    private fun loadCategories(categories: List<CategoryItem>) {
        val adapter = CategoryAdapter(categories) { category ->
            // When a category is selected, launch the Bottom Sheet dialog.
            val bottomSheet = TransactionDetailBottomSheet.newInstance(category, isIncomeSelected)
            bottomSheet.onTransactionSaved = {
                Toast.makeText(requireContext(), "Transaction saved", Toast.LENGTH_SHORT).show()
            }
            bottomSheet.show(parentFragmentManager, "TransactionDetailBottomSheet")
        }
        binding.rvCategories.adapter = adapter
    }

    // RecyclerView Adapter for category items.
    inner class CategoryAdapter(
        private val categories: List<CategoryItem>,
        private val onItemClick: (CategoryItem) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            // Get the TextView from the inflated item layout.
            val tvCategory: TextView = view.findViewById(R.id.tvCategoryItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
            return CategoryViewHolder(view)
        }

        override fun getItemCount(): Int = categories.size

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            // Set emoji on the first line and category name on the second, separated by newline.
            holder.tvCategory.text = "${category.emoji}\n${category.name}"
            // Center both the emoji and text.
            holder.tvCategory.gravity = Gravity.CENTER
            holder.itemView.setOnClickListener { onItemClick(category) }
        }
    }

    // Load the saved transactions from SharedPreferences.
    private fun loadTransactions() {
        val json = sharedPref.getString("transactions", null)
        transactionList = if (json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
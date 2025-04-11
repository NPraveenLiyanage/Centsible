package com.example.centsible

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.centsible.databinding.ActivityAddTransactionBinding

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private val sharedPref by lazy {
        getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)
    }

    // Combined Expense Categories (original + new requested ones)
    private val expenseCategories = listOf(
        // Previously defined expense categories:
        CategoryItem("Food", "🍔"),
        CategoryItem("Transport", "🚞"),
        CategoryItem("Bills", "💶"),
        CategoryItem("Entertainment", "🎬"),
        CategoryItem("Shopping", "🛍️"),
        CategoryItem("Health", "🏥"),
        CategoryItem("Travel", "✈️"),
        CategoryItem("Utilities", "💡"),
        CategoryItem("Education", "🎓"),
        // Additional requested expense categories:
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

    // Already defined income categories remain intact.
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

    // Flag indicating whether the current tab is Income (true) or Expense (false)
    private var isIncomeSelected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // (Optional) Hide any inline form if it exists—in our new design we rely on the Bottom Sheet.
        binding.formLayout.visibility = View.GONE

        // Configure the TabLayout with two tabs: Expense and Income.
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Expense"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Income"))

        // Setup RecyclerView using a GridLayoutManager (3 columns) for category selection.
        binding.rvCategories.layoutManager = GridLayoutManager(this, 3)
        // Initially load expense categories.
        loadCategories(expenseCategories)
        isIncomeSelected = false

        // Listen for tab selection changes.
        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                if (tab != null) {
                    when (tab.position) {
                        0 -> { // Expense tab
                            loadCategories(expenseCategories)
                            isIncomeSelected = false
                        }
                        1 -> { // Income tab
                            loadCategories(incomeCategories)
                            isIncomeSelected = true
                        }
                    }
                    // (Optional) No inline form to clear since all details are now entered in a Bottom Sheet.
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    // Instead of revealing a long scrollable form, we launch a Bottom Sheet dialog for transaction details.
    private fun loadCategories(categories: List<CategoryItem>) {
        val adapter = CategoryAdapter(categories) { category ->
            // When a category is selected, create and show the Bottom Sheet.
            val bottomSheet = TransactionDetailBottomSheet.newInstance(category, isIncomeSelected)
            bottomSheet.onTransactionSaved = {
                // Optionally perform actions (e.g., refresh data or finish the activity).
                finish()
            }
            bottomSheet.show(supportFragmentManager, "TransactionDetailBottomSheet")
        }
        binding.rvCategories.adapter = adapter
    }

    // RecyclerView Adapter for displaying category items.
    inner class CategoryAdapter(
        private val categories: List<CategoryItem>,
        private val onItemClick: (CategoryItem) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
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
            holder.tvCategory.text = "${category.emoji} ${category.name}"
            holder.itemView.setOnClickListener { onItemClick(category) }
        }
    }
}
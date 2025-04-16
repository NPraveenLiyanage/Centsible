package com.example.centsible.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.centsible.databinding.ItemCategoryAnalysisBinding

class CategoryAnalysisAdapter(
    private val categoryTotals: Map<String, Double>,
    private val onCategoryClick: ((String) -> Unit)? = null // Added click listener parameter.
) : RecyclerView.Adapter<CategoryAnalysisAdapter.CategoryViewHolder>() {

    private val categories = categoryTotals.keys.toList()

    // Mapping categories to their associated emojis.
    private val categoryEmojis = mapOf(
        "Food" to "🍔",
        "Transport" to "🚗",
        "Bills" to "💳",
        "Entertainment" to "🎬",
        "Shopping" to "🛍️",
        "Health" to "🏥",
        "Travel" to "✈️",
        "Utilities" to "🔌",
        "Education" to "🎓",
        "Phone" to "📱",
        "Beauty" to "💄",
        "Sports" to "⚽",
        "Social" to "👥",
        "Clothing" to "👗",
        "Car" to "🚗",
        "Alcohol" to "🍺",
        "Electronics" to "📺",
        "Pets" to "🐶",
        "Repair" to "🔧",
        "Housing" to "🏠",
        "Home" to "🏡",
        "Gift" to "🎁",
        "Donation" to "🤝",
        "Kids" to "👶",
        "Other Expense" to "💸",
        "Salary" to "💰",
        "Business" to "🏢",
        "Investments" to "💵",
        "Freelance" to "💻",
        "Rental Income" to "🏠",
        "Interest" to "💲",
        "Dividends" to "💳",
        "Other Income" to "🎁"
    )

    inner class CategoryViewHolder(val binding: ItemCategoryAnalysisBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryAnalysisBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.binding.tvCategory.text = category
        val total = categoryTotals[category] ?: 0.0
        holder.binding.tvTotal.text = "Total: LKR${String.format("%.2f", total)}"

        // Look up and set the emoji for the category.
        // Ensure your layout contains a TextView with the id tvCategoryAnalysisIcon.
        val emoji = categoryEmojis[category] ?: "❓"
        holder.binding.tvCategoryAnalysisIcon.text = emoji

        // Set the item clickable.
        holder.binding.root.setOnClickListener {
            onCategoryClick?.invoke(category)
        }
    }
}
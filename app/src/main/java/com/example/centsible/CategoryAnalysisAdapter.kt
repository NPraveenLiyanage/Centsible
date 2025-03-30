package com.example.centsible

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.centsible.databinding.ItemCategoryAnalysisBinding

class CategoryAnalysisAdapter(
    private val categoryTotals: Map<String, Double>
) : RecyclerView.Adapter<CategoryAnalysisAdapter.CategoryViewHolder>() {

    private val categories = categoryTotals.toList() // List of Pair(category, total)

    inner class CategoryViewHolder(val binding: ItemCategoryAnalysisBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryAnalysisBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val (category, total) = categories[position]
        with(holder.binding) {
            tvCategory.text = category
            tvTotal.text = "$$total"
        }
    }
}
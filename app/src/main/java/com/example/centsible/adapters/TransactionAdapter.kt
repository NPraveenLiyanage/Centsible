package com.example.centsible.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.centsible.Transaction
import com.example.centsible.databinding.ItemTransactionBinding

class TransactionAdapter(
    private var transactions: MutableList<Transaction>,
    private val onItemClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

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

    init {
        // enable stable IDs so that RecyclerView can optimize changes.
        setHasStableIds(true)
    }

    inner class TransactionViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            with(binding) {
                tvCategoryIcon.text = categoryEmojis[transaction.category] ?: "❓"
                // Bind transaction details.
                tvTitle.text = transaction.title
                tvDate.text = transaction.date
                val currencySymbol = com.example.centsible.CurrencyManager.getCurrencySymbol(root.context)
                tvAmount.text = if (transaction.isIncome)
                {
                    "+$currencySymbol${transaction.amount}"
                }else {
                    "-$currencySymbol${transaction.amount}"
                }
                //  display the category summary.
                tvTransactionSummary.text = transaction.category
                tvTransactionSummary.visibility = if (transaction.category.isNotEmpty()) View.VISIBLE else View.GONE

                root.setOnClickListener { onItemClick(transaction) }
                ibDelete.setOnClickListener { onDeleteClick(transaction) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemId(position: Int): Long {
        return transactions[position].id.hashCode().toLong()
    }
    
    fun updateData(newTransactions: MutableList<Transaction>) {
        val diffCallback = TransactionDiffCallback(transactions, newTransactions)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        transactions.clear()
        transactions.addAll(newTransactions)
        diffResult.dispatchUpdatesTo(this)
    }
}

class TransactionDiffCallback(
    private val oldList: List<Transaction>,
    private val newList: List<Transaction>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    // Compare the unique IDs.
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    // Compare the full content of the items.
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
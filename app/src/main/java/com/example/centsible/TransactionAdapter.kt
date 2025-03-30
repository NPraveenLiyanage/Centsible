package com.example.centsible

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.centsible.databinding.ItemTransactionBinding

class TransactionAdapter(
    private var transactions: MutableList<Transaction>,
    private val onItemClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        with(holder.binding) {
            tvTitle.text = transaction.title
            tvDate.text = transaction.date
            tvAmount.text = if (transaction.isIncome)
                "+$${transaction.amount}" else "-$${transaction.amount}"
            // Set click listeners
            root.setOnClickListener { onItemClick(transaction) }
            ibDelete.setOnClickListener { onDeleteClick(transaction) }
        }
    }

    /**
     * Updates the data using DiffUtil to calculate minimal changes.
     */
    fun updateData(newTransactions: MutableList<Transaction>) {
        val diffCallback = TransactionDiffCallback(transactions, newTransactions)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        transactions.clear()
        transactions.addAll(newTransactions)
        diffResult.dispatchUpdatesTo(this)
    }
}

/**
 * Helper class that defines how to compute the differences between two lists.
 */
class TransactionDiffCallback(
    private val oldList: List<Transaction>,
    private val newList: List<Transaction>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    // Compares the unique IDs of each Transaction.
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    // Compares the full content of Transactions.
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
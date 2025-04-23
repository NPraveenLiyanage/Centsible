package com.example.centsible.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.centsible.R
import com.example.centsible.databinding.FragmentBudgetUpdateBinding
import com.example.centsible.fragments.RecordsFragment
import androidx.core.content.edit

class BudgetUpdateFragment : Fragment() {

    private var _binding: FragmentBudgetUpdateBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPref: SharedPreferences
    private val BUDGET_KEY = "monthly_budget"

    companion object {
        fun newInstance(): BudgetUpdateFragment = BudgetUpdateFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBudgetUpdateBinding.inflate(inflater, container, false)
        sharedPref =
            requireContext().getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set input type for budget EditText.
        binding.etBudget.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        // Prepopulate budget if available.
        val currentBudget = sharedPref.getFloat(BUDGET_KEY, 0.0f)
        if (currentBudget != 0.0f) {
            binding.etBudget.setText(currentBudget.toString())
        }

        // Save the new budget when the button is pressed.
        binding.btnSaveBudget.setOnClickListener {
            val budgetText = binding.etBudget.text.toString().trim()
            if (budgetText.isNotEmpty()) {
                val newBudget = budgetText.toDoubleOrNull()
                if (newBudget != null) {
                    if (newBudget == 0.0 || newBudget < 1000) {
                        Toast.makeText(requireContext(), "Invalid budget entry", Toast.LENGTH_SHORT).show()
                    } else {
                        sharedPref.edit() { putFloat(BUDGET_KEY, newBudget.toFloat()) }
                        Toast.makeText(requireContext(), "Budget updated: $newBudget", Toast.LENGTH_SHORT).show()
                        redirectToRecordsFragment()
                    }
                } else {
                    Toast.makeText(requireContext(), "Invalid budget entry", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Budget cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun redirectToRecordsFragment() {
        // Replace the current fragment with a new instance of RecordsFragment.
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RecordsFragment.newInstance())
            .commit()

        // Additionally, update bottom navigation selection in MainActivity.
        (activity as? com.example.centsible.MainActivity)?.updateBottomNavSelection(R.id.nav_records)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
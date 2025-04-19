package com.example.centsible.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.centsible.DailyReminderWorker
import com.example.centsible.LoginActivity
import com.example.centsible.R
import com.example.centsible.databinding.FragmentBackupRestoreBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

class BackupRestoreFragment : Fragment() {

    private var _binding: FragmentBackupRestoreBinding? = null
    private val binding get() = _binding!!

    // SharedPreferences used for settings and data.
    private val sharedPref by lazy {
        requireContext().getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)
    }
    private val transactionsKey = "transactions"
    private val backupFileName = "transaction_backup.json"
    private val gson = Gson()

    // Keys for various settings.
    private val KEY_BUDGET_ALERT = "budget_alert_enabled"
    private val KEY_DAILY_REMINDER = "daily_reminder_enabled"
    private val currencyKey = "selected_currency"

    companion object {
        fun newInstance() = BackupRestoreFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Backup and Restore buttons.
        binding.btnBackup.setOnClickListener { backupData() }
        binding.btnRestore.setOnClickListener { restoreData() }

        // Initialize the Budget Alert switch.
        binding.switchBudgetAlert.isChecked = sharedPref.getBoolean(KEY_BUDGET_ALERT, true)
        binding.switchBudgetAlert.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean(KEY_BUDGET_ALERT, isChecked).apply()
            Toast.makeText(
                requireContext(),
                "Budget Alert notifications ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Initialize the Daily Reminder switch.
        binding.switchDailyReminder.isChecked = sharedPref.getBoolean(KEY_DAILY_REMINDER, true)
        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean(KEY_DAILY_REMINDER, isChecked).apply()
            Toast.makeText(
                requireContext(),
                "Daily Reminder notifications ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
            if (isChecked) {
                scheduleDailyReminder()
            } else {
                cancelDailyReminder()
            }
        }

        // Initialize Logout button.
        binding.btnLogout.setOnClickListener {
            sharedPref.edit().remove("current_user").apply()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        // Initialize Feedback button.
        binding.btnFeedback.setOnClickListener { showFeedbackDialog() }

        // Initialize the Currency Spinner.
        setupCurrencySpinner()
    }

    private fun setupCurrencySpinner() {
        // List of available currencies with symbols.
        val currencies = listOf("LKR (Rs)","USD ($)", "EUR (€)", "GBP (£)", "INR (₹)", "JPY (¥)")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter

        // Retrieve previously saved currency; default to "USD ($)".
        val savedCurrency = sharedPref.getString(currencyKey, "LKR (Rs)")
        val position = currencies.indexOf(savedCurrency)
        if (position >= 0) {
            binding.spinnerCurrency.setSelection(position)
        }

        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                val selected = currencies[pos]
                sharedPref.edit().putString(currencyKey, selected).apply()
                // Optionally, trigger a UI refresh in fragments/adapters using currency.
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun scheduleDailyReminder() {
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "DailyExpenseReminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }

    private fun cancelDailyReminder() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("DailyExpenseReminder")
    }

    private fun backupData() {
        val data = sharedPref.getString(transactionsKey, null)
        if (data != null) {
            try {
                requireContext().openFileOutput(backupFileName, Context.MODE_PRIVATE).use { fos ->
                    fos.write(data.toByteArray())
                }
                showMessage("Backup successful!")
            } catch (e: Exception) {
                showMessage("Backup failed: ${e.localizedMessage}")
            }
        } else {
            showMessage("No transaction data to backup.")
        }
    }

    private fun restoreData() {
        try {
            requireContext().openFileInput(backupFileName).bufferedReader().use { reader ->
                val data = reader.readText()
                with(sharedPref.edit()) {
                    putString(transactionsKey, data)
                    apply()
                }
            }
            showMessage("Restore successful!")
        } catch (e: Exception) {
            showMessage("Restore failed: ${e.localizedMessage}")
        }
    }

    private fun showMessage(message: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // Shows a custom feedback dialog (using a custom layout in dialog_feedback.xml).
    private fun showFeedbackDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_feedback, null)
        val etFeedback = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFeedback)
        val btnSend = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSendFeedback)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelFeedback)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnSend.setOnClickListener {
            val feedbackText = etFeedback.text.toString().trim()
            if (feedbackText.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:neeeleee6@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback for Centsible")
                    putExtra(Intent.EXTRA_TEXT, feedbackText)
                }
                try {
                    startActivity(Intent.createChooser(intent, "Send Feedback via Email"))
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Feedback cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
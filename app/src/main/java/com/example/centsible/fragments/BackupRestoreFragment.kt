package com.example.centsible.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private val sharedPref by lazy {
        requireContext().getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)
    }
    private val transactionsKey = "transactions"
    private val backupFileName = "transaction_backup.json"
    private val gson = Gson()

    // Keys for the new settings
    private val KEY_BUDGET_ALERT = "budget_alert_enabled"
    private val KEY_DAILY_REMINDER = "daily_reminder_enabled"

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

        // Initialize backup and restore buttons.
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

            // Schedule or cancel the daily reminder using DailyReminderWorker.
            if (isChecked) {
                scheduleDailyReminder()
            } else {
                cancelDailyReminder()
            }
        }

        // Initialize Logout button.
        binding.btnLogout.setOnClickListener {
            // Clear the current user.
            sharedPref.edit().remove("current_user").apply()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            // Navigate to LoginActivity.
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            // Finish the current activity to prevent the user from returning via back press.
            requireActivity().finish()
        }

        // Initialize Feedback button.
        binding.btnFeedback.setOnClickListener { showFeedbackDialog() }
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

    // Shows a feedback dialog that collects user feedback and sends it via an email intent.
    private fun showFeedbackDialog() {
        // Inflate the custom layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_feedback, null)
        // Find views in the custom layout
        val etFeedback = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFeedback)
        val btnSend = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSendFeedback)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelFeedback)

        // Create the dialog without using builder.setPositiveButton / setNegativeButton,
        // so we can use our own buttons.
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set click listener for Send button
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

        // Set click listener for Cancel button
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
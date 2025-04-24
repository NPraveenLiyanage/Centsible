package com.example.centsible.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import com.example.centsible.Transaction
import com.example.centsible.databinding.FragmentBackupRestoreBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class BackupRestoreFragment : Fragment() {

    private var _binding: FragmentBackupRestoreBinding? = null
    private val binding get() = _binding!!

    private val sharedPref by lazy {
        requireContext().getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE)
    }
    private val transactionsKey = "transactions"
    private val backupFileName = "transaction_backup.json"
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

        binding.btnExportExcel.setOnClickListener {
            val transactionsJson = sharedPref.getString(transactionsKey, "[]")
            val transactionType = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = Gson().fromJson(transactionsJson, transactionType)
            val success = exportTransactionsToExcel(requireContext(), transactions)
            if (success) {
                Toast.makeText(requireContext(), "Export successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Export failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCurrencySpinner() {
        val currencies = listOf("LKR (Rs)","USD ($)", "EUR (€)", "GBP (£)", "INR (₹)", "JPY (¥)")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter

        val savedCurrency = sharedPref.getString(currencyKey, "LKR (Rs)")
        val position = currencies.indexOf(savedCurrency)
        if (position >= 0) {
            binding.spinnerCurrency.setSelection(position)
        }

        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                val selected = currencies[pos]
                sharedPref.edit().putString(currencyKey, selected).apply()
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

    // Shows a feedback dialog .
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

    fun exportTransactionsToExcel(context: Context, transactions: List<Transaction>): Boolean {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Transactions")

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Title")
            headerRow.createCell(1).setCellValue("Date")
            headerRow.createCell(2).setCellValue("Amount")
            headerRow.createCell(3).setCellValue("Category")
            headerRow.createCell(4).setCellValue("Type")

            for ((index, transaction) in transactions.withIndex()) {
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(transaction.title)
                row.createCell(1).setCellValue(transaction.date)
                row.createCell(2).setCellValue(transaction.amount)
                row.createCell(3).setCellValue(transaction.category)
                row.createCell(4).setCellValue(if (transaction.isIncome) "Income" else "Expense")
            }


            for (i in 0..4) {
                sheet.setColumnWidth(i, 15 * 256)
            }

            val fileName = "Transactions.xlsx"
            val downloadsDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { fos ->
                workbook.write(fos)
            }
            workbook.close()

            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)

            Log.d("ExportExcel", "Excel file saved at: ${file.absolutePath}")

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
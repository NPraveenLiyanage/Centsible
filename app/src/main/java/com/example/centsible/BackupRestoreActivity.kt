package com.example.centsible

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.centsible.databinding.ActivityBackupRestoreBinding
import com.google.gson.Gson
import java.io.*

class BackupRestoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupRestoreBinding
    private val sharedPref by lazy { getSharedPreferences("PersonalFinancePrefs", Context.MODE_PRIVATE) }
    private val transactionsKey = "transactions"
    private val backupFileName = "transaction_backup.json"
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackup.setOnClickListener { backupData() }
        binding.btnRestore.setOnClickListener { restoreData() }
    }

    private fun backupData() {
        // Read transactions stored as JSON from SharedPreferences.
        val data = sharedPref.getString(transactionsKey, null)
        if (data != null) {
            try {
                // Open a file output stream in private mode.
                val fileOutputStream: FileOutputStream = openFileOutput(backupFileName, Context.MODE_PRIVATE)
                fileOutputStream.write(data.toByteArray())
                fileOutputStream.close()
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
            // Open the backup file for reading.
            val fileInputStream: FileInputStream = openFileInput(backupFileName)
            val inputStreamReader = InputStreamReader(fileInputStream)
            val data = inputStreamReader.readText()
            inputStreamReader.close()
            // Save the retrieved JSON back to SharedPreferences.
            val editor = sharedPref.edit()
            editor.putString(transactionsKey, data)
            editor.apply()
            showMessage("Restore successful!")
        } catch (e: Exception) {
            showMessage("Restore failed: ${e.localizedMessage}")
        }
    }

    private fun showMessage(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
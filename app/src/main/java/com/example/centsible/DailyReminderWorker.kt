package com.example.centsible

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

class DailyReminderWorker(appContext: Context, workerParameters: WorkerParameters) :
    Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        // Log to verify execution
        Log.d("DailyReminderWorker", "Worker is running, sending notification.")

        // Create an instance of NotificationHelper and send the daily reminder
        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.sendBudgetAlertNotification(
            title = "Daily Expense Reminder",
            message = "Don't forget to add your daily expenses!"
        )

        // Indicate whether the work finished successfully
        return Result.success()
    }
}
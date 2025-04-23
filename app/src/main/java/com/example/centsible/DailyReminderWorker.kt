package com.example.centsible

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

class DailyReminderWorker(appContext: Context, workerParameters: WorkerParameters) :
    Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        Log.d("DailyReminderWorker", "Worker is running, sending notification.")

        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.sendBudgetAlertNotification(
            title = "Daily Expense Reminder",
            message = "Don't forget to add your daily expenses!"
        )

        return Result.success()
    }
}
package com.farmapp.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.farmapp.FarmApplication
import com.farmapp.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class VaccinationReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val vaccineName = inputData.getString(KEY_VACCINE_NAME) ?: return Result.failure()
        val batchName = inputData.getString(KEY_BATCH_NAME) ?: return Result.failure()
        val vaccinationId = inputData.getLong(KEY_VACCINATION_ID, -1)

        sendNotification(batchName, vaccineName, vaccinationId)
        return Result.success()
    }

    private fun sendNotification(batchName: String, vaccineName: String, vaccinationId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "poultry")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            vaccinationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FarmApplication.CHANNEL_VACCINATION)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🐔 Vaccination Due Today!")
            .setContentText("$batchName needs $vaccineName today")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your flock '$batchName' is due for $vaccineName vaccination today. Tap to open the app.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(vaccinationId.toInt(), notification)
    }

    companion object {
        const val KEY_VACCINE_NAME = "vaccine_name"
        const val KEY_BATCH_NAME = "batch_name"
        const val KEY_VACCINATION_ID = "vaccination_id"

        fun schedule(
            context: Context,
            vaccinationId: Long,
            batchName: String,
            vaccineName: String,
            daysFromNow: Long
        ) {
            if (daysFromNow < 0) return

            val data = workDataOf(
                KEY_VACCINE_NAME to vaccineName,
                KEY_BATCH_NAME to batchName,
                KEY_VACCINATION_ID to vaccinationId
            )

            val request = OneTimeWorkRequestBuilder<VaccinationReminderWorker>()
                .setInitialDelay(daysFromNow, TimeUnit.DAYS)
                .setInputData(data)
                .addTag("vaccination_$vaccinationId")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "vaccination_$vaccinationId",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context, vaccinationId: Long) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("vaccination_$vaccinationId")
        }
    }
}

package com.farmapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FarmApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val vaccinationChannel = NotificationChannel(
                CHANNEL_VACCINATION,
                "Vaccination Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming poultry vaccinations"
            }

            val inventoryChannel = NotificationChannel(
                CHANNEL_INVENTORY,
                "Inventory Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Low stock inventory alerts"
            }

            manager.createNotificationChannel(vaccinationChannel)
            manager.createNotificationChannel(inventoryChannel)
        }
    }

    companion object {
        const val CHANNEL_VACCINATION = "vaccination_reminders"
        const val CHANNEL_INVENTORY = "inventory_alerts"
    }
}

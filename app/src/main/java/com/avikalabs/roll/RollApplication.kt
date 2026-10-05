package com.avikalabs.roll

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.avikalabs.roll.data.repository.RollRepository
import com.avikalabs.roll.receiver.UnlockReceiver
import com.avikalabs.roll.service.RollUnlockService

class RollApplication : Application() {

    private var unlockReceiver: UnlockReceiver? = null

    override fun onCreate() {
        super.onCreate()
        // Initialize Repository instance
        RollRepository.getInstance(this)

        // Register unlock receiver in code while app process is running
        registerUnlockReceiver()

        // Sync background unlock monitoring service state
        RollUnlockService.updateServiceState(this)
    }

    private fun registerUnlockReceiver() {
        val receiver = UnlockReceiver()
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
            unlockReceiver = receiver
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


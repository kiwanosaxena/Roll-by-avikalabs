package com.avikalabs.roll

import android.app.Application
import com.avikalabs.roll.billing.BillingManager
import com.avikalabs.roll.data.repository.RollRepository

class RollApplication : Application() {

    lateinit var billingManager: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize Repository instance
        RollRepository.getInstance(this)
        // Initialize BillingManager instance
        billingManager = BillingManager.getInstance(this)
    }
}

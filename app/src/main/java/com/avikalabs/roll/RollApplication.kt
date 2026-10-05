package com.avikalabs.roll

import android.app.Application
import com.avikalabs.roll.data.repository.RollRepository

class RollApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Repository instance
        RollRepository.getInstance(this)
    }
}


package com.example

import android.app.Application
import com.example.data.repository.RollRepository

class RollApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Repository instance
        RollRepository.getInstance(this)
    }
}

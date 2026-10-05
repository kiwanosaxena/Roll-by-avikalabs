package com.avikalabs.roll.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.avikalabs.roll.MainActivity
import com.avikalabs.roll.R
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.data.repository.RollRepository
import com.avikalabs.roll.receiver.UnlockReceiver
import com.avikalabs.roll.wallpaper.WallpaperApplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RollUnlockService : Service() {

    private var unlockReceiver: UnlockReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        registerUnlockReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = RollRepository.getInstance(applicationContext)
            val lockConfig = repository.getRotationConfigSync(WallpaperTarget.LOCK)
            val isLiveWallpaper = WallpaperApplier.isLiveWallpaperActive(applicationContext)
            val shouldRun = (lockConfig?.autoChangeOnUnlock == true) && !isLiveWallpaper
            if (!shouldRun) {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun registerUnlockReceiver() {
        if (unlockReceiver == null) {
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

    private fun unregisterUnlockReceiver() {
        unlockReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            unlockReceiver = null
        }
    }

    override fun onDestroy() {
        unregisterUnlockReceiver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lock Screen Auto-Change",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors device unlock to rotate lock screen wallpaper"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Roll")
            .setContentText("Auto-changing lock screen wallpaper on unlock")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "roll_unlock_channel"
        const val NOTIFICATION_ID = 1002

        fun updateServiceState(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = RollRepository.getInstance(context)
                    val lockConfig = repository.getRotationConfigSync(WallpaperTarget.LOCK)
                    val isLiveWallpaper = WallpaperApplier.isLiveWallpaperActive(context)
                    val needsForegroundService = (lockConfig?.autoChangeOnUnlock == true) && !isLiveWallpaper

                    val intent = Intent(context, RollUnlockService::class.java)
                    if (needsForegroundService) {
                        try {
                            ContextCompat.startForegroundService(context, intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            context.stopService(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

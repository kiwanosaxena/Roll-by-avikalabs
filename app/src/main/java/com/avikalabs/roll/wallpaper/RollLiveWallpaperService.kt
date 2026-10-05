package com.avikalabs.roll.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.data.repository.RollRepository
import com.avikalabs.roll.receiver.UnlockReceiver
import com.avikalabs.roll.service.RollUnlockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class RollLiveWallpaperService : WallpaperService() {

    private var unlockReceiver: UnlockReceiver? = null

    override fun onCreate() {
        super.onCreate()
        registerUnlockReceiver()
        RollUnlockService.updateServiceState(applicationContext)
    }

    override fun onDestroy() {
        unregisterUnlockReceiver()
        super.onDestroy()
        RollUnlockService.updateServiceState(applicationContext)
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

    override fun onCreateEngine(): Engine = RollEngine()

    inner class RollEngine : Engine() {

        private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private val repository by lazy { RollRepository.getInstance(applicationContext) }

        @Volatile
        private var currentBitmap: Bitmap? = null
        private var lastRenderedUri: String? = null

        private val gestureDetector by lazy {
            GestureDetector(this@RollLiveWallpaperService, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (isPreview) return false // never advance during system live-wallpaper preview

                    serviceScope.launch(Dispatchers.IO) {
                        val doubleTapEnabled = repository.isHomeDoubleTapEnabledSync()
                        if (!doubleTapEnabled) return@launch

                        val items = repository.getItemsByTargetSync(WallpaperTarget.HOME)
                        if (items.isEmpty()) return@launch

                        // Advance rotation without resetting running schedule per PRD §7.5
                        repository.advanceRotation(WallpaperTarget.HOME, isScheduledTrigger = false)
                    }
                    return true
                }
            })
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // Enable touch events for double-tap gesture
            setTouchEventsEnabled(true)
            observeWallpaperChanges()
        }

        private fun observeWallpaperChanges() {
            serviceScope.launch {
                combine(
                    repository.getItems(WallpaperTarget.HOME),
                    repository.getRotationConfig(WallpaperTarget.HOME)
                ) { items, config ->
                    Pair(items, config)
                }.collectLatest { (items, config) ->
                    if (items.isNotEmpty()) {
                        val currentIndex = config?.currentIndex?.coerceIn(0, items.size - 1) ?: 0
                        val targetItem = items[currentIndex]
                        loadAndRender(targetItem.uri)
                    } else {
                        currentBitmap = null
                        lastRenderedUri = null
                        renderToSurface(null)
                    }
                }
            }
        }

        private suspend fun loadAndRender(uri: String) {
            if (uri == lastRenderedUri && currentBitmap != null) {
                renderToSurface(currentBitmap)
                return
            }

            val bitmap = withContext(Dispatchers.IO) {
                WallpaperApplier.loadPreviewBitmap(
                    applicationContext,
                    uri,
                    1080,
                    1920
                )
            }

            currentBitmap = bitmap
            lastRenderedUri = uri
            renderToSurface(bitmap)
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestureDetector.onTouchEvent(event)
            super.onTouchEvent(event)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            renderToSurface(currentBitmap)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                // When returning to visible, ensure latest wallpaper is drawn
                serviceScope.launch {
                    val items = withContext(Dispatchers.IO) { repository.getItemsByTargetSync(WallpaperTarget.HOME) }
                    val config = withContext(Dispatchers.IO) { repository.getRotationConfigSync(WallpaperTarget.HOME) }
                    if (items.isNotEmpty()) {
                        val currentIndex = config?.currentIndex?.coerceIn(0, items.size - 1) ?: 0
                        loadAndRender(items[currentIndex].uri)
                    } else {
                        renderToSurface(currentBitmap)
                    }
                }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            renderToSurface(currentBitmap)
        }

        private fun renderToSurface(bitmap: Bitmap?) {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    if (bitmap != null) {
                        val canvasWidth = canvas.width.toFloat()
                        val canvasHeight = canvas.height.toFloat()
                        val srcWidth = bitmap.width.toFloat()
                        val srcHeight = bitmap.height.toFloat()

                        // Center crop aspect ratio calculation to prevent distortion
                        val scale = max(canvasWidth / srcWidth, canvasHeight / srcHeight)
                        val scaledWidth = srcWidth * scale
                        val scaledHeight = srcHeight * scale
                        val left = (canvasWidth - scaledWidth) / 2f
                        val top = (canvasHeight - scaledHeight) / 2f

                        val dstRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
                        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                        canvas.drawBitmap(bitmap, null, dstRect, paint)
                    } else {
                        // Sleek dark fallback if no photos configured yet
                        canvas.drawColor(Color.parseColor("#10151D"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        override fun onDestroy() {
            serviceScope.cancel()
            currentBitmap = null
            super.onDestroy()
        }
    }
}

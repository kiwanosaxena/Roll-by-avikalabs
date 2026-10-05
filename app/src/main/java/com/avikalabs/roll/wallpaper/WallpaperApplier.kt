package com.avikalabs.roll.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.avikalabs.roll.data.model.WallpaperTarget
import java.io.InputStream
import kotlin.math.max

object WallpaperApplier {

    fun isLiveWallpaperActive(context: Context): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val info = wallpaperManager.wallpaperInfo
            info != null && info.packageName == context.packageName
        } catch (_: Exception) {
            false
        }
    }

    fun launchLiveWallpaperPicker(context: Context) {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, RollLiveWallpaperService::class.java)
                )
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun applyWallpaper(context: Context, imageUriString: String, target: WallpaperTarget): Boolean {
        // If this is HOME screen and Roll Live Wallpaper is active:
        // Do NOT call wallpaperManager.setBitmap(FLAG_SYSTEM) because setting a static bitmap
        // on FLAG_SYSTEM causes Android to unset and destroy the active Live Wallpaper!
        if (target == WallpaperTarget.HOME && isLiveWallpaperActive(context)) {
            // RollLiveWallpaperService is running and renders automatically via Flow updates
            return true
        }

        return try {
            val uri = Uri.parse(imageUriString)
            val screenDims = getScreenDimensions(context)
            val bitmap = decodeSampledBitmapFromUri(context, uri, screenDims.first, screenDims.second)
                ?: return false

            val croppedBitmap = centerCropBitmap(bitmap, screenDims.first, screenDims.second)

            val wallpaperManager = WallpaperManager.getInstance(context)
            val flag = if (target == WallpaperTarget.HOME) {
                WallpaperManager.FLAG_SYSTEM
            } else {
                WallpaperManager.FLAG_LOCK
            }

            wallpaperManager.setBitmap(croppedBitmap, null, true, flag)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadPreviewBitmap(context: Context, imageUriString: String, reqWidth: Int = 400, reqHeight: Int = 600): Bitmap? {
        return try {
            val uri = Uri.parse(imageUriString)
            decodeSampledBitmapFromUri(context, uri, reqWidth, reqHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getScreenDimensions(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val metrics = DisplayMetrics()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm?.currentWindowMetrics?.bounds
            if (bounds != null) {
                Pair(bounds.width(), bounds.height())
            } else {
                context.resources.displayMetrics.let { Pair(it.widthPixels, it.heightPixels) }
            }
        } else {
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealMetrics(metrics)
            Pair(metrics.widthPixels, metrics.heightPixels)
        }
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            inputStream?.close()
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun centerCropBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (src.width == targetWidth && src.height == targetHeight) return src

        val srcWidth = src.width.toFloat()
        val srcHeight = src.height.toFloat()

        val scale = max(targetWidth / srcWidth, targetHeight / srcHeight)
        val scaledWidth = srcWidth * scale
        val scaledHeight = srcHeight * scale

        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(src, null, destRect, paint)
        return output
    }
}

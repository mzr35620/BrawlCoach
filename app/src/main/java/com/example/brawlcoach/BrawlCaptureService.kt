
package com.example.brawlcoach

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import java.nio.ByteBuffer

class BrawlCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "capture_data"

        const val ACTION_STATUS =
            "com.example.brawlcoach.CAPTURE_STATUS"
        const val EXTRA_STATUS = "status"

        private const val CHANNEL_ID = "brawl_capture"
        private const val NOTIFICATION_ID = 2002
        private const val OCR_INTERVAL_MS = 1200L
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var frameReceived = false
    private var ocrInProgress = false
    private var captureActive = false
    private var lastOcrTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendStatus(message: String) {
        val intent = Intent(ACTION_STATUS).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATUS, message)
        }
        sendBroadcast(intent)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(
            EXTRA_RESULT_CODE,
            Activity.RESULT_CANCELED
        )

        @Suppress("DEPRECATION")
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)

        if (resultCode != Activity.RESULT_OK || data == null) {
            sendStatus("Autorisation de capture invalide.")
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Brawl Coach")
            .setContentText("Capture active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val manager = getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

            projection = manager.getMediaProjection(resultCode, data)

            projection?.registerCallback(
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        releaseResources()
                        sendStatus("Capture arrêtée.")
                        stopSelf()
                    }
                },
                null
            )

            startCapture()

        } catch (e: Exception) {
            sendStatus("Erreur de capture : ${e.message}")
            stopCapture()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()

        @Suppress("DEPRECATION")
        val windowManager =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager

        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        frameReceived = false
        captureActive = true
        ocrInProgress = false
        lastOcrTime = 0L

        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            var image: Image? = null

            try {
                image = reader.acquireLatestImage()

                if (image != null && captureActive) {
                    val bitmap = imageToBitmap(image)

                    if (bitmap != null) {
                        if (!frameReceived) {
                            frameReceived = true
                            sendStatus("Image reçue. Analyse locale active.")
                        }

                        analyzeFrame(bitmap)
                    }
                }
            } catch (e: Exception) {
                sendStatus("Erreur de lecture de l'image.")
            } finally {
                image?.close()
            }
        }, null)

        virtualDisplay = projection?.createVirtualDisplay(
            "BrawlCoachCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        if (virtualDisplay != null) {
            sendStatus("Capture démarrée. En attente de l'image.")
        } else {
            sendStatus("Impossible de démarrer la capture.")
        }
    }

    private fun analyzeFrame(bitmap: Bitmap) {
        val now = SystemClock.elapsedRealtime()

        if (!captureActive ||
            ocrInProgress ||
            now - lastOcrTime < OCR_INTERVAL_MS
        ) {
            bitmap.recycle()
            return
        }

        ocrInProgress = true
        lastOcrTime = now

        BrawlOcrAnalyzer.analyze(bitmap) { result ->

            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            ocrInProgress = false

            if (!captureActive) return@analyze

            result.onSuccess { names ->
                if (names.isNotEmpty()) {
                    sendStatus(
                        "Brawlers détectés : ${names.joinToString(", ")}"
                    )
                }
            }

            result.onFailure {
                sendStatus("Analyse OCR indisponible.")
            }
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        if (image.planes.isEmpty()) return null

        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride

        val width = image.width
        val height = image.height

        if (width <= 0 || height <= 0) return null

        val pixels = IntArray(width * height)
        val rowPadding = rowStride - pixelStride * width

        if (pixelStride < 4 || rowPadding < 0) return null

        val rowBuffer = ByteArray(rowStride * height)
        buffer.rewind()
        buffer.get(
            rowBuffer,
            0,
            minOf(buffer.remaining(), rowBuffer.size)
        )

        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = y * rowStride + x * pixelStride

                val r = rowBuffer[offset].toInt() and 0xFF
                val g = rowBuffer[offset + 1].toInt() and 0xFF
                val b = rowBuffer[offset + 2].toInt() and 0xFF
                val a = rowBuffer[offset + 3].toInt() and 0xFF

                pixels[y * width + x] =
                    (a shl 24) or
                    (r shl 16) or
                    (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(
            pixels,
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
    }

    private fun releaseResources() {
        captureActive = false

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
    }

    private fun stopCapture() {
        releaseResources()

        val currentProjection = projection
        projection = null
        currentProjection?.stop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Capture Brawl Coach",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}


package com.example.brawlcoach

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var panel: View? = null
    private var body: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var statusText: TextView? = null
    private var minimized = false
    private var receiverRegistered = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BrawlCaptureService.ACTION_STATUS) {
                val message = intent.getStringExtra(
                    BrawlCaptureService.EXTRA_STATUS
                ) ?: return

                statusText?.text = "●  $message"
            }
        }
    }

    private fun registerStatusReceiver() {
        if (receiverRegistered) return

        val filter = IntentFilter(BrawlCaptureService.ACTION_STATUS)

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                statusReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, filter)
        }

        receiverRegistered = true
    }

    private fun background(
        color: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER_VERTICAL
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        registerStatusReceiver()

        if (panel == null) {
            createPanel()
        }

        return START_NOT_STICKY
    }

    private fun createPanel() {
        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 14)
            background = GradientDrawable().apply {
                colors = intArrayOf(
                    Color.rgb(24, 29, 49),
                    Color.rgb(13, 17, 31)
                )
                orientation =
                    GradientDrawable.Orientation.TOP_BOTTOM
                cornerRadius = 24f
                setStroke(2, Color.rgb(124, 77, 255))
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val dragHandle = TextView(this).apply {
            text = "☰  BRAWL COACH"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(10, 14, 10, 14)
            background = background(
                Color.rgb(48, 53, 83),
                14f
            )
        }

        val reduce = makeText(
            "−",
            25f,
            Color.WHITE
        ).apply {
            gravity = Gravity.CENTER
            setPadding(12, 5, 12, 5)
        }

        val close = makeText(
            "×",
            25f,
            Color.rgb(255, 100, 120)
        ).apply {
            gravity = Gravity.CENTER
            setPadding(12, 5, 8, 5)
        }

        header.addView(
            dragHandle,
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        header.addView(reduce)
        header.addView(close)

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 14, 4, 4)
        }

        statusText = makeText(
            "●  ASSISTANT ACTIF",
            13f,
            Color.rgb(100, 240, 170)
        )

        val separator = View(this).apply {
            setBackgroundColor(Color.rgb(65, 70, 100))
        }

        val footer = makeText(
            "BRAWL COACH • VERSION 2",
            10f,
            Color.rgb(150, 155, 180)
        )

        body?.addView(statusText)

        body?.addView(
            separator,
            LinearLayout.LayoutParams(-1, 1).apply {
                topMargin = 10
                bottomMargin = 10
            }
        )

        body?.addView(footer)

        root.addView(header)
        root.addView(body)

        reduce.setOnClickListener {
            minimized = !minimized
            body?.visibility =
                if (minimized) View.GONE else View.VISIBLE

            reduce.text = if (minimized) "+" else "−"
        }

        close.setOnClickListener {
            stopSelf()
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = 24
            y = 140
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        dragHandle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params?.x ?: 0
                    startY = params?.y ?: 0
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params?.let {
                        it.x =
                            startX + (event.rawX - touchX).toInt()
                        it.y =
                            startY + (event.rawY - touchY).toInt()

                        windowManager?.updateViewLayout(root, it)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    true
                }

                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }

        panel = root
        windowManager?.addView(root, params)
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(statusReceiver)
            receiverRegistered = false
        }

        panel?.let {
            windowManager?.removeView(it)
        }

        panel = null
        statusText = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

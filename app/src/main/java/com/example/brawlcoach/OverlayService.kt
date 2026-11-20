
package com.example.brawlcoach

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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
    private var minimized = false

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (panel == null) createPanel()

        return START_NOT_STICKY
    }

    private fun createPanel() {

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            background = GradientDrawable().apply {
                setColor(Color.rgb(20, 27, 43))
                cornerRadius = 28f
                setStroke(2, Color.rgb(124, 77, 255))
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "⚡ BRAWL COACH"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(4, 8, 12, 8)
        }

        val reduce = TextView(this).apply {
            text = "−"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(12, 0, 12, 0)
        }

        val close = TextView(this).apply {
            text = "×"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(255, 100, 100))
            setPadding(12, 0, 8, 0)
        }

        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(reduce)
        header.addView(close)

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 12, 4, 4)
        }

        val status = TextView(this).apply {
            text = "● Assistant actif"
            textSize = 14f
            setTextColor(Color.rgb(100, 230, 160))
        }

        val info = TextView(this).apply {
            text = "Prêt pour une partie !"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 4)
        }

        body?.addView(status)
        body?.addView(info)

        root.addView(header)
        root.addView(body)

        reduce.setOnClickListener {
            minimized = !minimized
            body?.visibility = if (minimized) View.GONE else View.VISIBLE
            reduce.text = if (minimized) "+" else "−"
        }

        close.setOnClickListener {
            stopSelf()
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        header.setOnTouchListener { _, event ->
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
                        it.x = startX + (event.rawX - touchX).toInt()
                        it.y = startY + (event.rawY - touchY).toInt()
                        windowManager?.updateViewLayout(root, it)
                    }
                    true
                }

                else -> false
            }
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

        panel = root
        windowManager?.addView(root, params)
    }

    override fun onDestroy() {
        panel?.let {
            windowManager?.removeView(it)
        }
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

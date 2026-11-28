
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
    private var resultsContainer: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var statusText: TextView? = null

    private var minimized = false
    private var receiverRegistered = false

    private var detectedMode: String? = null
    private var detectedMap: String? = null

    private val green = Color.rgb(65, 230, 145)
    private val white = Color.WHITE
    private val muted = Color.rgb(170, 180, 195)

    private val statusReceiver = object : BroadcastReceiver() {

        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {
            if (intent?.action != BrawlCaptureService.ACTION_STATUS) {
                return
            }

            val message = intent.getStringExtra(
                BrawlCaptureService.EXTRA_STATUS
            ) ?: ""

            statusText?.text = "●  $message"

            detectedMode = intent.getStringExtra(
                BrawlCaptureService.EXTRA_MODE
            )?.takeIf { it.isNotBlank() }

            detectedMap = intent.getStringExtra(
                BrawlCaptureService.EXTRA_MAP
            )?.takeIf { it.isNotBlank() }

            renderModeMap()
        }
    }

    private fun registerStatusReceiver() {

        if (receiverRegistered) return

        val filter = IntentFilter(
            BrawlCaptureService.ACTION_STATUS
        )

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

    private fun makeBackground(
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

        val detection = BrawlModeMapStore.latestDetection

        detectedMode = detection?.modeName
        detectedMap = detection?.mapName

        if (panel == null) {
            createPanel()
        }

        renderModeMap()

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
                    Color.rgb(22, 34, 43),
                    Color.rgb(12, 20, 29)
                )
                orientation =
                    GradientDrawable.Orientation.TOP_BOTTOM
                cornerRadius = 22f
                setStroke(2, green)
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val dragHandle = TextView(this).apply {
            text = "☰  BRAWL COACH"
            textSize = 15f
            setTextColor(white)
            setPadding(10, 14, 10, 14)
            background = makeBackground(
                Color.rgb(35, 65, 55),
                14f
            )
        }

        val reduce = makeText("−", 25f, white).apply {
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
            setPadding(4, 12, 4, 4)
        }

        statusText = makeText(
            "●  ASSISTANT ACTIF",
            12f,
            green
        )

        body?.addView(statusText)

        addSeparator()

        body?.addView(
            makeText(
                "🎮  ANALYSE DU CLASSÉ",
                13f,
                green
            )
        )

        resultsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        body?.addView(resultsContainer)

        addSeparator()

        body?.addView(
            makeText(
                "Analyse locale • BRAWL COACH",
                10f,
                muted
            )
        )

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

    private fun addSeparator() {

        val separator = View(this).apply {
            setBackgroundColor(Color.rgb(65, 85, 80))
        }

        body?.addView(
            separator,
            LinearLayout.LayoutParams(-1, 1).apply {
                topMargin = 9
                bottomMargin = 9
            }
        )
    }

    private fun renderModeMap() {

        val container = resultsContainer ?: return

        container.removeAllViews()

        val detection = BrawlModeMapStore.latestDetection

        if (detection != null) {
            detectedMode = detection.modeName
            detectedMap = detection.mapName
        }

        val mode = detectedMode
        val map = detectedMap

        container.addView(
            makeText(
                "Mode : ${mode ?: "Non reconnu"}",
                13f,
                if (mode != null) green else muted
            ).apply {
                setPadding(0, 5, 0, 5)
            }
        )

        container.addView(
            makeText(
                "Carte : ${map ?: "Non reconnue"}",
                13f,
                if (map != null) green else muted
            ).apply {
                setPadding(0, 5, 0, 5)
            }
        )

        when {

            mode != null && map != null -> {
                container.addView(
                    makeText(
                        "Mode et carte reconnus",
                        11f,
                        green
                    ).apply {
                        setPadding(0, 10, 0, 2)
                    }
                )
            }

            mode != null -> {
                container.addView(
                    makeText(
                        "Mode reconnu — analyse possible sans carte",
                        11f,
                        green
                    ).apply {
                        setPadding(0, 10, 0, 2)
                    }
                )
            }

            else -> {
                container.addView(
                    makeText(
                        "En attente de reconnaissance du mode...",
                        11f,
                        muted
                    ).apply {
                        setPadding(0, 10, 0, 2)
                    }
                )
            }
        }
    }

    override fun onDestroy() {

        if (receiverRegistered) {
            unregisterReceiver(statusReceiver)
            receiverRegistered = false
        }

        panel?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Fenêtre déjà retirée.
            }
        }

        panel = null
        statusText = null
        body = null
        resultsContainer = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

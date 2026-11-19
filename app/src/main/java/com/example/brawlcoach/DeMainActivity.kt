package com.example.brawlcoach

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(17, 24, 39))
        }
        val title = TextView(this).apply {
            text = "BRAWL COACH"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Assistant de draft — prototype"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        }
        val info = TextView(this).apply {
            text = "Choisis tes combattants et consulte des conseils de composition. La reconnaissance automatique du jeu n'est pas encore intégrée."
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        val overlay = Button(this).apply {
            text = "Autoriser la superposition"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                }
            }
        }
        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(info)
        layout.addView(overlay)
        setContentView(layout)
    }
}

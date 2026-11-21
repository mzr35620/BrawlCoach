
package com.example.brawlcoach

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var result: TextView
    private lateinit var loadButton: Button

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
            text = "Assistant de draft"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        }

        val overlay = Button(this).apply {
            text = "Autoriser la superposition"

            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                } else {
                    startService(
                        Intent(this@MainActivity, OverlayService::class.java)
                    )
                }
            }
        }

        loadButton = Button(this).apply {
            text = "Charger les données BrawlAPI"
            setOnClickListener {
                loadBrawlData()
            }
        }

        result = TextView(this).apply {
            text = "Données non chargées."
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 20, 8, 20)
        }

        val scroll = ScrollView(this).apply {
            addView(result)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(overlay)
        layout.addView(loadButton)
        layout.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(layout)
    }

    private fun loadBrawlData() {
        loadButton.isEnabled = false
        loadButton.text = "Chargement..."
        result.text = "Connexion à BrawlAPI..."

        Thread {
            try {
                val brawlers = BrawlApi.getBrawlers()
                val maps = BrawlApi.getMaps()

                val activeBrawlers = brawlers.size
                val activeMaps = maps.count { !it.disabled }

                val display = StringBuilder()

                display.append("CONNEXION RÉUSSIE\n\n")
                display.append("Combattants : $activeBrawlers\n")
                display.append("Cartes disponibles : $activeMaps\n\n")

                display.append("QUELQUES COMBATTANTS\n")

                brawlers.take(12).forEach {
                    display.append("• ${it.name}\n")
                }

                display.append("\nQUELQUES CARTES\n")

                maps.filter { !it.disabled }.take(12).forEach {
                    display.append("• ${it.name} (${it.gameMode})\n")
                }

                runOnUiThread {
                    result.text = display.toString()
                    loadButton.text = "Actualiser les données"
                    loadButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    result.text =
                        "Erreur de connexion : ${e.message}"

                    loadButton.text = "Réessayer"
                    loadButton.isEnabled = true
                }
            }
        }.start()
    }
}


package com.example.brawlcoach

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.widget.*
 
class MainActivity : Activity() {

    private lateinit var result: TextView
    private lateinit var loadButton: Button
    private lateinit var modeSpinner: Spinner
    private lateinit var mapSpinner: Spinner

    private var allMaps: List<BrawlMap> = emptyList()
    private var allModes: List<BrawlGameMode> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(17, 24, 39))
        }

        fun makeLabel(value: String): TextView {
            return TextView(this).apply {
                text = value
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(0, 14, 0, 6)
            }
        }

        val title = TextView(this).apply {
            text = "BRAWL COACH"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = makeLabel("Assistant de draft")

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

        val modeLabel = makeLabel("MODE DE JEU")
        modeSpinner = Spinner(this)

        val mapLabel = makeLabel("CARTE")
        mapSpinner = Spinner(this)

        result = TextView(this).apply {
            text = "Charge les données pour commencer."
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 20, 8, 20)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(overlay)
        layout.addView(loadButton)
        layout.addView(modeLabel)
        layout.addView(modeSpinner)
        layout.addView(mapLabel)
        layout.addView(mapSpinner)
        layout.addView(result)

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
                val modes = BrawlApi.getGameModes()

                runOnUiThread {
                    allMaps = maps.filter { !it.disabled }
                    allModes = modes.filter { !it.disabled }

                    val modeNames = allModes.map { it.name }
                        .filter { it.isNotBlank() }
                        .distinct()

                    if (modeNames.isEmpty()) {
                        result.text = "Aucun mode disponible."
                    } else {
                        val adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_spinner_item,
                            modeNames
                        )

                        adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )

                        modeSpinner.adapter = adapter

                        modeSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: android.view.View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    updateMaps(modeNames[position])
                                }

                                override fun onNothingSelected(
                                    parent: AdapterView<*>?
                                ) {}
                            }

                        result.text =
                            "Connexion réussie !\n\n" +
                            "Combattants : ${brawlers.size}\n" +
                            "Cartes disponibles : ${allMaps.size}\n" +
                            "Modes disponibles : ${modeNames.size}\n\n" +
                            "Choisis ton mode et ta carte."
                    }

                    loadButton.text = "Actualiser les données"
                    loadButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    result.text = "Erreur : ${e.message}"
                    loadButton.text = "Réessayer"
                    loadButton.isEnabled = true
                }
            }
        }.start()
    }

    private fun updateMaps(modeName: String) {
        val filteredMaps = allMaps.filter {
            it.gameMode.equals(modeName, ignoreCase = true)
        }

        val mapNames = filteredMaps.map { it.name }
            .filter { it.isNotBlank() }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            if (mapNames.isEmpty()) listOf("Aucune carte disponible")
            else mapNames
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        mapSpinner.adapter = adapter

        result.text =
            "Mode : $modeName\n" +
            "Cartes trouvées : ${mapNames.size}\n\n" +
            if (mapNames.isEmpty()) {
                "Aucune carte associée à ce mode dans les données."
            } else {
                "Carte sélectionnée : ${mapNames.first()}"
            }

        mapSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (mapNames.isNotEmpty()) {
                        result.text =
                            "Mode : $modeName\n" +
                            "Carte sélectionnée : ${mapNames[position]}\n\n" +
                            "Sélection prête."
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }
    }
}

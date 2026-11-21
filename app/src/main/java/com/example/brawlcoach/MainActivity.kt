
package com.example.brawlcoach

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var result: TextView
    private lateinit var loadButton: Button
    private lateinit var modeSpinner: Spinner
    private lateinit var mapSpinner: Spinner

    private val teamSpinners = mutableListOf<Spinner>()
    private val enemySpinners = mutableListOf<Spinner>()

    private var allMaps: List<BrawlMap> = emptyList()
    private var allModes: List<BrawlGameMode> = emptyList()
    private var allBrawlers: List<Brawler> = emptyList()

    private var selectedMode = ""
    private var selectedMap = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
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
            setOnClickListener { loadBrawlData() }
        }

        modeSpinner = Spinner(this)
        mapSpinner = Spinner(this)

        result = TextView(this).apply {
            text = "Charge les données pour commencer."
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 20, 8, 20)
        }

        content.addView(title)
        content.addView(subtitle)
        content.addView(overlay)
        content.addView(loadButton)
        content.addView(makeLabel("MODE DE JEU"))
        content.addView(modeSpinner)
        content.addView(makeLabel("CARTE"))
        content.addView(mapSpinner)

        content.addView(makeLabel("TON ÉQUIPE"))
        repeat(3) { index ->
            content.addView(makeLabel("Coéquipier ${index + 1}"))
            val spinner = Spinner(this)
            teamSpinners.add(spinner)
            content.addView(spinner)
        }

        content.addView(makeLabel("ÉQUIPE ADVERSE"))
        repeat(3) { index ->
            content.addView(makeLabel("Adversaire ${index + 1}"))
            val spinner = Spinner(this)
            enemySpinners.add(spinner)
            content.addView(spinner)
        }

        content.addView(result)

        val scrollView = ScrollView(this)
        scrollView.addView(content)
        setContentView(scrollView)
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
                    allBrawlers = brawlers
                    allMaps = maps.filter { !it.disabled }
                    allModes = modes.filter { !it.disabled }

                    val modeNames = allModes.map { it.name }
                        .filter { it.isNotBlank() }
                        .distinct()

                    val brawlerNames = listOf("Non choisi") +
                        allBrawlers.map { it.name }.distinct()

                    val brawlerAdapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        brawlerNames
                    )

                    brawlerAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                    )

                    (teamSpinners + enemySpinners).forEach { spinner ->
                        spinner.adapter = brawlerAdapter
                        spinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    updateSummary()
                                }

                                override fun onNothingSelected(
                                    parent: AdapterView<*>?
                                ) {}
                            }
                    }

                    val modeAdapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        modeNames
                    )

                    modeAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                    )

                    modeSpinner.adapter = modeAdapter

                    modeSpinner.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                if (modeNames.isNotEmpty()) {
                                    selectedMode = modeNames[position]
                                    updateMaps(selectedMode)
                                }
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
                        "Choisis ton mode, ta carte et les combattants."

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

        mapSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedMap = if (mapNames.isNotEmpty()) {
                        mapNames[position]
                    } else {
                        ""
                    }
                    updateSummary()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }
    }

    private fun updateSummary() {
        if (allBrawlers.isEmpty()) return

        val team = teamSpinners.mapIndexedNotNull { index, spinner ->
            spinner.selectedItem?.toString()
                ?.takeIf { it != "Non choisi" }
                ?.let { "Coéquipier ${index + 1} : $it" }
        }

        val enemies = enemySpinners.mapIndexedNotNull { index, spinner ->
            spinner.selectedItem?.toString()
                ?.takeIf { it != "Non choisi" }
                ?.let { "Adversaire ${index + 1} : $it" }
        }

        result.text =
            "MODE : ${selectedMode.ifBlank { "Non choisi" }}\n" +
            "CARTE : ${selectedMap.ifBlank { "Non choisie" }}\n\n" +
            "TON ÉQUIPE\n" +
            (team.joinToString("\n").ifBlank { "Aucun choix" }) +
            "\n\nÉQUIPE ADVERSE\n" +
            (enemies.joinToString("\n").ifBlank { "Aucun choix" }) +
            "\n\nSélection enregistrée."
    }
}

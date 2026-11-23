
package com.example.brawlcoach

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build

class MainActivity : Activity() {

    private lateinit var result: TextView
    private lateinit var loadButton: Button
    private lateinit var recommendButton: Button
    private lateinit var modeSpinner: Spinner
    private lateinit var mapSpinner: Spinner

    private val teamButtons = mutableListOf<Button>()
    private val enemyButtons = mutableListOf<Button>()

    private val teamChoices = MutableList(3) { "Non choisi" }
    private val enemyChoices = MutableList(3) { "Non choisi" }

    private var allMaps: List<BrawlMap> = emptyList()
    private var allModes: List<BrawlGameMode> = emptyList()
    private var allBrawlers: List<Brawler> = emptyList()

    // Images des Brawlers conservées en mémoire
    private var brawlerImages: Map<String, Bitmap> = emptyMap()

    private var selectedMode = ""
    private var selectedMap = ""

    private var captureResultCode: Int = Activity.RESULT_CANCELED
    private var captureData: Intent? = null

    companion object {
        private const val REQUEST_CAPTURE = 2001
    }

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

        val captureButton = Button(this).apply {
            text = "Préparer l'analyse de Brawl Stars"

            setOnClickListener {
                val intent = createCaptureIntent()

                if (intent == null) {
                    result.text =
                        "Cette fonction nécessite Android 14 ou supérieur."
                    return@setOnClickListener
                }

                result.text =
                    "Autorisation Android demandée.\n" +
                    "Choisis uniquement Brawl Stars."

                startActivityForResult(intent, REQUEST_CAPTURE)
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

        recommendButton = Button(this).apply {
            text = "TROUVER LES 3 MEILLEURS COMBATTANTS"
            setOnClickListener { recommendBrawlers() }
            isEnabled = false
        }

        content.addView(title)
        content.addView(subtitle)
        content.addView(overlay)
        content.addView(captureButton)
        content.addView(loadButton)

        content.addView(makeLabel("MODE DE JEU"))
        content.addView(modeSpinner)

        content.addView(makeLabel("CARTE"))
        content.addView(mapSpinner)

        content.addView(makeLabel("TON ÉQUIPE"))

        repeat(3) { index ->
            content.addView(makeLabel("Coéquipier ${index + 1}"))

            val button = Button(this).apply {
                text = "Non choisi"
                setOnClickListener {
                    showBrawlerPicker(index, true)
                }
            }

            teamButtons.add(button)
            content.addView(button)
        }

        content.addView(makeLabel("ÉQUIPE ADVERSE"))

        repeat(3) { index ->
            content.addView(makeLabel("Adversaire ${index + 1}"))

            val button = Button(this).apply {
                text = "Non choisi"
                setOnClickListener {
                    showBrawlerPicker(index, false)
                }
            }

            enemyButtons.add(button)
            content.addView(button)
        }

        content.addView(recommendButton)
        content.addView(result)

        val scrollView = ScrollView(this)
        scrollView.addView(content)
        setContentView(scrollView)
    }

    private fun createCaptureIntent(): Intent? {
        if (Build.VERSION.SDK_INT < 34) {
            return null
        }

        val manager = getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        return manager.createScreenCaptureIntent(
            MediaProjectionConfig.createConfigForUserChoice()
        )
    }

    @Deprecated("Utilisé pour recevoir l'autorisation MediaProjection")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAPTURE) {

            if (resultCode == Activity.RESULT_OK && data != null) {

                captureResultCode = resultCode
                captureData = data

                val serviceIntent = Intent(
                    this,
                    BrawlCaptureService::class.java
                ).apply {
                    putExtra(
                        BrawlCaptureService.EXTRA_RESULT_CODE,
                        resultCode
                    )
                    putExtra(
                        BrawlCaptureService.EXTRA_DATA,
                        data
                    )
                }

                try {
                    if (Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }

                    result.text =
                        "Autorisation accordée !\n\n" +
                        "Démarrage du service de capture demandé."

                    captureData = null

                } catch (e: Exception) {
                    result.text =
                        "Erreur au démarrage : ${e.message}"
                }

            } else {
                captureResultCode = Activity.RESULT_CANCELED
                captureData = null

                result.text =
                    "Autorisation refusée ou annulée.\n" +
                    "Aucune capture ne sera effectuée."
            }
        }
    }

    private fun createSpinnerAdapter(
        values: List<String>
    ): ArrayAdapter<String> {

        return object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            values
        ) {

            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val view = super.getView(
                    position,
                    convertView,
                    parent
                ) as TextView

                view.setTextColor(Color.WHITE)
                view.textSize = 18f
                view.setPadding(16, 12, 16, 12)

                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val view = super.getDropDownView(
                    position,
                    convertView,
                    parent
                ) as TextView

                view.setTextColor(Color.WHITE)
                view.textSize = 18f
                view.setBackgroundColor(Color.rgb(31, 41, 55))
                view.setPadding(20, 16, 20, 16)

                return view
            }
        }.apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }
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

                val downloadedImages =
                    BrawlerImageRepository.downloadAll(brawlers)

                runOnUiThread {
                    allBrawlers = brawlers
                    allMaps = maps.filter { !it.disabled }
                    allModes = modes.filter { !it.disabled }

                    brawlerImages = downloadedImages

                    val modeNames = allModes.map { it.name }
                        .filter { it.isNotBlank() }
                        .distinct()

                    modeSpinner.adapter =
                        createSpinnerAdapter(modeNames)

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

                    recommendButton.isEnabled = true

                    result.text =
                        "Connexion réussie !\n\n" +
                        "Combattants : ${brawlers.size}\n" +
                        "Images téléchargées : ${brawlerImages.size}\n" +
                        "Cartes : ${allMaps.size}\n" +
                        "Modes : ${modeNames.size}\n\n" +
                        "Choisis tes combattants."

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

    private fun showBrawlerPicker(index: Int, isTeam: Boolean) {
        if (allBrawlers.isEmpty()) {
            Toast.makeText(
                this,
                "Charge d'abord les données BrawlAPI.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val names = arrayOf("Non choisi") +
            allBrawlers.map { it.name }.distinct().toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(
                if (isTeam) "Choisir un coéquipier"
                else "Choisir un adversaire"
            )
            .setItems(names) { _, position ->

                val choice = names[position]

                if (isTeam) {
                    teamChoices[index] = choice
                    teamButtons[index].text = choice
                } else {
                    enemyChoices[index] = choice
                    enemyButtons[index].text = choice
                }

                updateSummary()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun updateMaps(modeName: String) {
        val mapNames = allMaps.filter {
            it.gameMode.equals(modeName, ignoreCase = true)
        }.map { it.name }.filter { it.isNotBlank() }

        val values = if (mapNames.isEmpty()) {
            listOf("Aucune carte disponible")
        } else {
            mapNames
        }

        mapSpinner.adapter = createSpinnerAdapter(values)

        mapSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedMap = mapNames.getOrElse(position) { "" }
                    updateSummary()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }
    }

    private fun recommendBrawlers() {

        if (allBrawlers.isEmpty()) {
            result.text = "Charge d'abord les données."
            return
        }

        val unavailable = (teamChoices + enemyChoices)
            .filter { it != "Non choisi" }
            .toSet()

        val teamClasses = teamChoices
            .filter { it != "Non choisi" }
            .mapNotNull { name ->
                allBrawlers.find { it.name == name }
                    ?.className
                    ?.takeIf { it.isNotBlank() }
            }

        val enemyClasses = enemyChoices
            .filter { it != "Non choisi" }
            .mapNotNull { name ->
                allBrawlers.find { it.name == name }
                    ?.className
                    ?.takeIf { it.isNotBlank() }
            }

        val teamCounts = teamClasses
            .groupingBy { it.trim().lowercase() }
            .eachCount()

        val enemyCounts = enemyClasses
            .groupingBy { it.trim().lowercase() }
            .eachCount()

        val recommendations = allBrawlers
            .filter { it.name !in unavailable }
            .map { brawler ->

                val role = brawler.className.trim().lowercase()

                var score = 50

                if (role.isNotBlank()) {
                    val teamCount = teamCounts[role] ?: 0
                    score += if (teamCount == 0) 20 else -10 * teamCount
                    if (teamCount == 0) score += 5
                    if (teamCount >= 2) score -= 10

                    val enemyCount = enemyCounts[role] ?: 0
                    if (enemyCount == 0) score += 8
                    else score -= 8 * enemyCount
                }

                Pair(brawler, score)
            }
            .sortedByDescending { it.second }
            .take(3)

        if (recommendations.isEmpty()) {
            result.text = "Aucune recommandation disponible."
            return
        }

        val message = buildString {
            append("🏆 TOP 3 DES RECOMMANDATIONS\n\n")

            recommendations.forEachIndexed { index, pair ->
                val brawler = pair.first
                val score = pair.second

                append("${index + 1}. ${brawler.name}\n")
                append("Classe : ")
                append(brawler.className.ifBlank { "Non renseignée" })
                append("\nScore de composition : $score\n\n")
            }

            append("📊 ANALYSE DE TON ÉQUIPE\n")

            if (teamClasses.isEmpty()) {
                append("Aucun coéquipier sélectionné.\n")
            } else {
                teamCounts.forEach { (role, count) ->
                    append("$role : $count\n")
                }
            }

            append("\n🔎 ANALYSE DES ADVERSAIRES\n")

            if (enemyClasses.isEmpty()) {
                append("Aucun adversaire sélectionné.\n")
            } else {
                enemyCounts.forEach { (role, count) ->
                    append("$role : $count\n")
                }
            }

            append("\nMode : ")
            append(selectedMode.ifBlank { "Non choisi" })
            append("\nCarte : ")
            append(selectedMap.ifBlank { "Non choisie" })

            append("\n\nLe score tient compte de la diversité ")
            append("de ton équipe et des classes adverses.")
            append("\nCe calcul est indicatif : les vrais contres ")
            append("et statistiques de victoire par carte ")
            append("ne sont pas encore intégrés.")
        }

        result.text = message
    }

    // Résumé avec compteur des portraits téléchargés
    private fun updateSummary() {
        result.text =
            "MODE : ${selectedMode.ifBlank { "Non choisi" }}\n" +
            "CARTE : ${selectedMap.ifBlank { "Non choisie" }}\n\n" +
            "PORTRAITS DES BRAWLERS\n" +
            "Images disponibles : ${brawlerImages.size} / ${allBrawlers.size}\n\n" +
            "TON ÉQUIPE\n" +
            teamChoices.mapIndexedNotNull { index, name ->
                if (name == "Non choisi") null
                else "Coéquipier ${index + 1} : $name"
            }.joinToString("\n").ifBlank { "Aucun choix" } +
            "\n\nÉQUIPE ADVERSE\n" +
            enemyChoices.mapIndexedNotNull { index, name ->
                if (name == "Non choisi") null
                else "Adversaire ${index + 1} : $name"
            }.joinToString("\n").ifBlank { "Aucun choix" } +
            "\n\nSélection enregistrée."
    }
}

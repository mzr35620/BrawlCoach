
package com.example.brawlcoach

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var result: TextView
    private lateinit var loadButton: Button
    private lateinit var captureButton: Button

    private var allBrawlers: List<Brawler> = emptyList()
    private var allMaps: List<BrawlMap> = emptyList()
    private var allModes: List<BrawlGameMode> = emptyList()

    private var selectedMode = ""
    private var selectedMap = ""

    private var lastRecommendationKey = ""

    companion object {
        private const val REQUEST_CAPTURE = 2001
    }

    private var receiverRegistered = false

    private val captureReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action != BrawlCaptureService.ACTION_STATUS) {
                return
            }

            val status = intent.getStringExtra(
                BrawlCaptureService.EXTRA_STATUS
            ) ?: "Analyse en cours..."

            // Chaque nouvelle analyse remplace les anciennes valeurs.
            selectedMode = intent.getStringExtra(
                BrawlCaptureService.EXTRA_MODE
            )?.takeIf { it.isNotBlank() } ?: ""

            selectedMap = intent.getStringExtra(
                BrawlCaptureService.EXTRA_MAP
            )?.takeIf { it.isNotBlank() } ?: ""

            if (selectedMode.isNotBlank() &&
                allBrawlers.isNotEmpty()
            ) {

                val key = "$selectedMode|$selectedMap"

                if (key != lastRecommendationKey) {
                    lastRecommendationKey = key
                    recommendBrawlers()
                }

            } else {

                if (selectedMode.isBlank()) {
                    lastRecommendationKey = ""
                }

                result.text =
                    "BRAWL COACH\n\n" +
                    "$status\n\n" +
                    "Mode : ${selectedMode.ifBlank { "En attente" }}\n" +
                    "Carte : ${selectedMap.ifBlank { "Non reconnue" }}\n\n" +
                    if (allBrawlers.isEmpty()) {
                        "Charge les données BrawlAPI."
                    } else {
                        "En attente de la détection d'un mode."
                    }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(17, 24, 39))
        }

        val title = TextView(this).apply {
            text = "BRAWL COACH"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 10)
        }

        val subtitle = TextView(this).apply {
            text = "Assistant automatique de draft"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 20)
        }

        val overlayButton = Button(this).apply {
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
                        Intent(
                            this@MainActivity,
                            OverlayService::class.java
                        )
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Superposition activée",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        captureButton = Button(this).apply {
            text = "DÉMARRER L'ANALYSE"

            setOnClickListener {
                val intent = createCaptureIntent()

                if (intent == null) {
                    result.text =
                        "La capture nécessite Android 14 ou supérieur."
                    return@setOnClickListener
                }

                result.text =
                    "Autorisation Android demandée.\n\n" +
                    "Choisis uniquement Brawl Stars."

                startActivityForResult(
                    intent,
                    REQUEST_CAPTURE
                )
            }
        }

        loadButton = Button(this).apply {
            text = "CHARGER LES DONNÉES BRAWLAPI"

            setOnClickListener {
                loadBrawlData()
            }
        }

        result = TextView(this).apply {
            text = "Charge les données BrawlAPI pour commencer."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(12, 30, 12, 30)
        }

        content.addView(title)
        content.addView(subtitle)
        content.addView(overlayButton)
        content.addView(captureButton)
        content.addView(loadButton)
        content.addView(result)

        val scrollView = ScrollView(this)
        scrollView.addView(content)

        setContentView(scrollView)
    }

    override fun onStart() {
        super.onStart()

        if (!receiverRegistered) {

            val filter = IntentFilter(
                BrawlCaptureService.ACTION_STATUS
            )

            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(
                    captureReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(captureReceiver, filter)
            }

            receiverRegistered = true
        }
    }

    override fun onStop() {

        if (receiverRegistered) {
            unregisterReceiver(captureReceiver)
            receiverRegistered = false
        }

        super.onStop()
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

        if (requestCode != REQUEST_CAPTURE) {
            return
        }

        if (resultCode == Activity.RESULT_OK && data != null) {

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
                    "Capture démarrée.\n\n" +
                    "En attente de la détection du mode."

            } catch (e: Exception) {

                result.text =
                    "Erreur au démarrage : ${e.message}"
            }

        } else {

            result.text =
                "Autorisation refusée ou annulée."
        }
    }

    private fun loadBrawlData() {

        loadButton.isEnabled = false
        loadButton.text = "CHARGEMENT..."
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

                    BrawlModeMapStore.setReferences(
                        newModes = allModes,
                        newMaps = allMaps
                    )

                    loadButton.isEnabled = true
                    loadButton.text = "ACTUALISER LES DONNÉES"

                    if (selectedMode.isNotBlank()) {
                        lastRecommendationKey = ""
                        recommendBrawlers()
                    } else {
                        result.text =
                            "DONNÉES CHARGÉES !\n\n" +
                            "Brawlers : ${allBrawlers.size}\n" +
                            "Cartes : ${allMaps.size}\n" +
                            "Modes : ${allModes.size}\n\n" +
                            "Tu peux démarrer l'analyse de Brawl Stars."
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    loadButton.isEnabled = true
                    loadButton.text = "RÉESSAYER"

                    result.text =
                        "Erreur BrawlAPI : ${e.message}"
                }
            }

        }.start()
    }

    private fun recommendBrawlers() {

        if (allBrawlers.isEmpty()) {
            result.text = "Charge d'abord les données BrawlAPI."
            return
        }

        if (selectedMode.isBlank()) {
            result.text = "En attente de la détection du mode."
            return
        }

        // La carte est facultative.
        val recommendations = BrawlCoachAI.recommend(
            brawlers = allBrawlers,
            modeName = selectedMode,
            mapName = selectedMap,
            allies = emptyList(),
            enemies = emptyList(),
            limit = 3
        )

        if (recommendations.isEmpty()) {
            result.text = "Aucune recommandation disponible."
            return
        }

        result.text = buildString {

            append("🏆 TOP 3 BRAWL COACH\n\n")

            append("Mode : $selectedMode\n")

            append(
                "Carte : ${
                    selectedMap.ifBlank {
                        "Non reconnue - analyse par mode"
                    }
                }\n\n"
            )

            recommendations.forEachIndexed { index, recommendation ->

                append("${index + 1}. ")
                append(recommendation.brawler.name)
                append("\n")

                append("Score : ")
                append(recommendation.score)
                append("\n")

                recommendation.reasons.forEach { reason ->
                    append("• $reason\n")
                }

                append("\n")
            }

            append("Analyse automatique terminée.\n\n")

            append(
                "Classement indicatif basé sur les règles " +
                "stratégiques intégrées à Brawl Coach."
            )
        }
    }
}

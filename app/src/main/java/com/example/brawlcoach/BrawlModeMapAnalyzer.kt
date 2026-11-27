
package com.example.brawlcoach

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.Normalizer
import java.util.Locale

object BrawlModeMapAnalyzer {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    private val aliases = mapOf(
        "gem grab" to listOf(
            "razzia de gemmes",
            "razzia de gemme",
            "razzia gemmes",
            "gem grab",
            "gemgrab"
        ),
        "brawl ball" to listOf(
            "brawlball",
            "brawl ball"
        ),
        "showdown" to listOf(
            "survivant",
            "survivant solo",
            "survivant duo",
            "solo showdown",
            "duo showdown",
            "showdown"
        ),
        "knockout" to listOf(
            "hors jeu",
            "hors-jeu",
            "knockout"
        ),
        "bounty" to listOf(
            "prime",
            "bounty"
        ),
        "heist" to listOf(
            "braquage",
            "heist"
        ),
        "hot zone" to listOf(
            "zone reservee",
            "zone réservée",
            "hot zone",
            "hotzone"
        ),
        "wipeout" to listOf(
            "chasse ouverte",
            "wipeout"
        ),
        "duels" to listOf(
            "duels",
            "duel"
        ),
        "basket brawl" to listOf(
            "basket brawl",
            "basketbrawl"
        ),
        "volley brawl" to listOf(
            "volley brawl",
            "volleybrawl"
        ),
        "brawl hockey" to listOf(
            "brawl hockey",
            "hockey brawl",
            "brawlhockey"
        ),
        "payload" to listOf(
            "cargaison",
            "payload"
        ),
        "big game" to listOf(
            "big game",
            "biggame"
        ),
        "boss fight" to listOf(
            "boss fight",
            "combat de boss",
            "bossfight"
        ),
        "robo rumble" to listOf(
            "robo rumble",
            "robots a gogo",
            "roborumble"
        )
    )

    private fun normalize(value: String): String {

        // Sépare les identifiants camelCase :
        // gemGrab devient gem Grab.
        val separated = value.replace(
            Regex("([a-z])([A-Z])"),
            "$1 $2"
        )

        return Normalizer.normalize(
            separated.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean {

        val normalizedText = normalize(text)
        val normalizedPhrase = normalize(phrase)

        if (normalizedPhrase.isBlank()) return false

        return " $normalizedText ".contains(
            " $normalizedPhrase "
        )
    }

    private fun containsMapName(
        text: String,
        mapName: String
    ): Boolean {

        val normalizedText = normalize(text)
        val normalizedName = normalize(mapName)

        if (normalizedName.isBlank()) return false

        // Correspondance normale.
        if (containsPhrase(normalizedText, normalizedName)) {
            return true
        }

        // Correspondance sans espaces pour les erreurs OCR.
        // On évite les noms trop courts pour limiter les faux positifs.
        val compactName = normalizedName.replace(" ", "")
        val compactText = normalizedText.replace(" ", "")

        return compactName.length >= 8 &&
            compactText.contains(compactName)
    }

    private fun detectMode(
        text: String,
        modes: List<BrawlGameMode>
    ): BrawlGameMode? {

        val candidates = mutableListOf<
            Pair<BrawlGameMode, String>
        >()

        for (mode in modes) {

            val normalizedName = normalize(mode.name)
            val normalizedHash = normalize(mode.scHash)

            if (normalizedName.isNotBlank()) {
                candidates.add(mode to normalizedName)

                aliases[normalizedName]?.forEach { alias ->
                    candidates.add(mode to normalize(alias))
                }
            }

            if (normalizedHash.isNotBlank()) {
                candidates.add(mode to normalizedHash)

                aliases[normalizedHash]?.forEach { alias ->
                    candidates.add(mode to normalize(alias))
                }
            }
        }

        return candidates
            .distinctBy { "${it.first.id}_${it.second}" }
            .sortedByDescending { it.second.length }
            .firstOrNull { (_, phrase) ->
                containsPhrase(text, phrase)
            }
            ?.first
    }

    private fun detectMap(
        text: String,
        maps: List<BrawlMap>,
        modeId: Int
    ): BrawlMap? {

        if (modeId <= 0) return null

        // On conserve uniquement les cartes du mode détecté.
        val compatibleMaps = maps.filter { map ->
            map.gameModeId == modeId &&
                map.name.isNotBlank() &&
                !map.disabled
        }

        return compatibleMaps
            .sortedByDescending {
                normalize(it.name).length
            }
            .firstOrNull { map ->
                containsMapName(text, map.name)
            }
    }

    fun analyze(
        bitmap: Bitmap,
        onResult: (Result<BrawlModeMapDetection>) -> Unit
    ) {

        if (bitmap.isRecycled) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Image indisponible."
                    )
                )
            )
            return
        }

        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->

                // Utilise tout le texte reconnu par ML Kit.
                val detectedText = normalize(
                    visionText.text
                )

                val modes = BrawlModeMapStore.modes
                val maps = BrawlModeMapStore.maps

                // 1. Détection du mode.
                val detectedMode = detectMode(
                    detectedText,
                    modes
                )

                // 2. Détection de la carte dans ce mode.
                val detectedMap = detectedMode?.let { mode ->
                    detectMap(
                        detectedText,
                        maps,
                        mode.id
                    )
                }

                onResult(
                    Result.success(
                        BrawlModeMapDetection(
                            modeName = detectedMode?.name,
                            mapName = detectedMap?.name,
                            detectedText = detectedText
                        )
                    )
                )
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun close() {
        recognizer.close()
    }
}

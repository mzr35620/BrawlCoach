
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
            "gem grab"
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
            "duo showdown"
        ),
        "knockout" to listOf(
            "hors jeu",
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
            "hot zone"
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
            "hockey brawl"
        ),
        "payload" to listOf(
            "cargaison",
            "payload"
        ),
        "big game" to listOf(
            "big game"
        ),
        "boss fight" to listOf(
            "boss fight",
            "combat de boss"
        ),
        "robo rumble" to listOf(
            "robo rumble",
            "robots a gogo"
        )
    )

    private fun normalize(value: String): String {
        return Normalizer.normalize(
            value.lowercase(Locale.ROOT),
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

        val normalizedPhrase = normalize(phrase)

        if (normalizedPhrase.isBlank()) return false

        val paddedText = " $text "
        val paddedPhrase = " $normalizedPhrase "

        return paddedText.contains(paddedPhrase)
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

            if (normalizedName.isNotBlank()) {
                candidates.add(mode to normalizedName)
            }

            val normalizedHash = normalize(mode.scHash)

            if (normalizedHash.isNotBlank()) {
                candidates.add(mode to normalizedHash)
            }

            aliases[normalizedName]?.forEach { alias ->
                candidates.add(mode to normalize(alias))
            }
        }

        // Les noms les plus longs sont testés en premier.
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

        // Seules les cartes appartenant à ce mode sont autorisées.
        val compatibleMaps = maps.filter {
            it.gameModeId == modeId &&
                it.name.isNotBlank() &&
                !it.disabled
        }

        // Pas de correspondance approximative :
        // le nom doit réellement être présent dans le texte.
        return compatibleMaps
            .sortedByDescending { normalize(it.name).length }
            .firstOrNull { map ->
                containsPhrase(
                    text,
                    normalize(map.name)
                )
            }
    }

    fun analyze(
        bitmap: Bitmap,
        onResult: (Result<BrawlModeMapDetection>) -> Unit
    ) {

        if (bitmap.isRecycled) {
            onResult(
                Result.failure(
                    IllegalArgumentException("Image indisponible.")
                )
            )
            return
        }

        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->

                val lines = visionText.textBlocks.flatMap { block ->
                    block.lines.map { it.text }
                }

                val detectedText = normalize(
                    lines.joinToString(" ")
                )

                val modes = BrawlModeMapStore.modes
                val maps = BrawlModeMapStore.maps

                // 1. Détection du mode.
                val detectedMode = detectMode(
                    detectedText,
                    modes
                )

                // 2. Détection de la carte dans ce mode uniquement.
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

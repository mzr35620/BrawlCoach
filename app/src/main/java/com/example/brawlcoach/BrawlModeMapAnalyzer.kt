
package com.example.brawlcoach

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

object BrawlModeMapAnalyzer {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    // Correspondances entre les noms français et anglais.
    private val modeAliases = mapOf(
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
            "survivant trio",
            "solo showdown",
            "duo showdown"
        ),

        "knockout" to listOf(
            "hors jeu",
            "horsjeu",
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
            "hockey brawl",
            "hockey"
        ),

        "siege" to listOf(
            "siege",
            "siege de robots"
        ),

        "payload" to listOf(
            "cargaison",
            "payload"
        ),

        "big game" to listOf(
            "big game",
            "gros jeu"
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

    private fun levenshtein(
        first: String,
        second: String
    ): Int {

        val previous = IntArray(second.length + 1) {
            it
        }

        val current = IntArray(second.length + 1)

        for (i in first.indices) {

            current[0] = i + 1

            for (j in second.indices) {

                val cost =
                    if (first[i] == second[j]) 0 else 1

                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + cost
                )
            }

            for (j in previous.indices) {
                previous[j] = current[j]
            }
        }

        return previous[second.length]
    }

    private fun similarity(
        first: String,
        second: String
    ): Double {

        if (first.isBlank() || second.isBlank()) {
            return 0.0
        }

        val distance = levenshtein(first, second)

        return 1.0 -
            distance.toDouble() /
            max(first.length, second.length)
    }

    private fun findBestMatch(
        names: List<String>,
        detectedText: String,
        aliases: Map<String, List<String>> = emptyMap(),
        threshold: Double = 0.78
    ): String? {

        val words = detectedText
            .split(" ")
            .filter { it.isNotBlank() }

        var bestName: String? = null
        var bestScore = 0.0

        for (originalName in names) {

            val normalizedName = normalize(originalName)

            if (normalizedName.isBlank()) continue

            val candidates = mutableListOf(normalizedName)

            // Ajout des traductions françaises.
            aliases[normalizedName]?.forEach { alias ->
                candidates.add(normalize(alias))
            }

            for (candidateName in candidates) {

                if (candidateName.isBlank()) continue

                // Correspondance exacte.
                if (detectedText.contains(candidateName)) {
                    return originalName
                }

                val expectedCount =
                    candidateName.split(" ").size

                for (
                    windowSize in
                    max(1, expectedCount - 1)..expectedCount + 1
                ) {

                    if (windowSize > words.size) continue

                    for (start in 0..words.size - windowSize) {

                        val candidate = words
                            .subList(start, start + windowSize)
                            .joinToString(" ")

                        val score = similarity(
                            candidateName,
                            candidate
                        )

                        if (score > bestScore) {
                            bestScore = score
                            bestName = originalName
                        }
                    }
                }
            }
        }

        return if (bestScore >= threshold) {
            bestName
        } else {
            null
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

                // Toutes les cartes disponibles et non désactivées.
                val maps = BrawlModeMapStore.maps
                    .filter {
                        it.name.isNotBlank() && !it.disabled
                    }

                // Tous les modes, y compris ceux hors rotation.
                val modes = BrawlModeMapStore.modes
                    .filter {
                        it.name.isNotBlank()
                    }

                // Recherche de la carte.
                val detectedMapName = findBestMatch(
                    names = maps.map { it.name },
                    detectedText = detectedText,
                    threshold = 0.82
                )

                val detectedMap = maps.firstOrNull {
                    it.name == detectedMapName
                }

                // Recherche du mode français ou anglais.
                val detectedModeName = findBestMatch(
                    names = modes.map { it.name },
                    detectedText = detectedText,
                    aliases = modeAliases,
                    threshold = 0.82
                )

                val detectedModeFromText = modes.firstOrNull {
                    it.name == detectedModeName
                }

                // Le mode associé à la carte sert de secours.
                val detectedModeFromMap =
                    detectedMap?.gameMode?.let { mapMode ->

                        modes.firstOrNull { mode ->
                            normalize(mode.name) ==
                                normalize(mapMode)
                        }
                    }

                val detectedMode =
                    detectedModeFromText ?: detectedModeFromMap

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

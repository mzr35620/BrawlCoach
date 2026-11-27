
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
        detectedText: String
    ): String? {

        val words = detectedText
            .split(" ")
            .filter { it.isNotBlank() }

        var bestName: String? = null
        var bestScore = 0.0

        for (originalName in names) {

            val name = normalize(originalName)

            if (name.isBlank()) continue

            // Correspondance exacte.
            if (detectedText.contains(name)) {
                return originalName
            }

            val nameWords = name.split(" ")
            val expectedCount = nameWords.size

            for (windowSize in
                max(1, expectedCount - 1)..expectedCount + 1
            ) {

                if (windowSize > words.size) continue

                for (start in 0..words.size - windowSize) {

                    val candidate = words
                        .subList(start, start + windowSize)
                        .joinToString(" ")

                    val score = similarity(name, candidate)

                    if (score > bestScore) {
                        bestScore = score
                        bestName = originalName
                    }
                }
            }
        }

        // Seuil de sécurité contre les fausses détections.
        return if (bestScore >= 0.78) {
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

                val maps = BrawlModeMapStore.maps
                    .filter {
                        it.name.isNotBlank() && !it.disabled
                    }

                val modes = BrawlModeMapStore.modes
                    .filter {
                        it.name.isNotBlank() && !it.disabled
                    }

                // Recherche de la carte, même avec quelques
                // erreurs de lecture OCR.
                val detectedMapName = findBestMatch(
                    maps.map { it.name },
                    detectedText
                )

                val detectedMap = maps.firstOrNull {
                    it.name == detectedMapName
                }

                // Recherche directe du mode.
                val detectedModeName = findBestMatch(
                    modes.map { it.name },
                    detectedText
                )

                val detectedModeFromText = modes.firstOrNull {
                    it.name == detectedModeName
                }

                // Le mode de la carte est une seconde source
                // de reconnaissance.
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

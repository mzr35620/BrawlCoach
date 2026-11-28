
package com.example.brawlcoach

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

object BrawlModeMapAnalyzer {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    // Noms français et anglais des modes.
    private val modeAliases = mapOf(
        "gem grab" to listOf(
            "razzia de gemmes",
            "razzia de gemme",
            "razzia gemmes",
            "gem grab",
            "gemgrab"
        ),
        "brawl ball" to listOf(
            "brawlball",
            "brawl ball",
            "balle brawl"
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
            "basketbrawl",
            "basket"
        ),
        "volley brawl" to listOf(
            "volley brawl",
            "volleybrawl",
            "volley"
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
            "biggame",
            "gros jeu"
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

    private fun compact(value: String): String {
        return normalize(value).replace(" ", "")
    }

    // Distance de Levenshtein pour tolérer les erreurs OCR.
    private fun levenshtein(
        first: String,
        second: String
    ): Int {

        if (first == second) return 0
        if (first.isEmpty()) return second.length
        if (second.isEmpty()) return first.length

        var previous = IntArray(second.length + 1) {
            it
        }

        for (i in first.indices) {

            val current = IntArray(second.length + 1)
            current[0] = i + 1

            for (j in second.indices) {

                val cost = if (
                    first[i] == second[j]
                ) 0 else 1

                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + cost
                )
            }

            previous = current
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

        val a = compact(first)
        val b = compact(second)

        if (a == b) return 1.0

        val distance = levenshtein(a, b)
        val longest = max(a.length, b.length)

        return 1.0 - distance.toDouble() / longest
    }

    private fun phraseScore(
        text: String,
        phrase: String
    ): Double {

        val normalizedText = normalize(text)
        val normalizedPhrase = normalize(phrase)

        if (
            normalizedText.isBlank() ||
            normalizedPhrase.isBlank()
        ) return 0.0

        if (
            " $normalizedText ".contains(
                " $normalizedPhrase "
            )
        ) {
            return 1.0
        }

        val compactText = compact(normalizedText)
        val compactPhrase = compact(normalizedPhrase)

        if (
            compactPhrase.length >= 5 &&
            compactText.contains(compactPhrase)
        ) {
            return 0.98
        }

        // Comparaison avec chaque ligne OCR.
        val lines = text.lines().filter {
            it.isNotBlank()
        }

        var best = 0.0

        for (line in lines) {
            best = max(
                best,
                similarity(line, phrase)
            )
        }

        // Comparaison avec deux lignes consécutives.
        for (i in 0 until lines.size - 1) {
            best = max(
                best,
                similarity(
                    "${lines[i]} ${lines[i + 1]}",
                    phrase
                )
            )
        }

        // Tolérance limitée pour les erreurs de lecture.
        val threshold = when {
            compactPhrase.length >= 12 -> 0.78
            compactPhrase.length >= 8 -> 0.84
            else -> 0.92
        }

        return if (best >= threshold) best else 0.0
    }

    private fun getModeCandidates(
        mode: BrawlGameMode
    ): List<String> {

        val candidates = mutableListOf<String>()

        candidates.add(mode.name)
        candidates.add(mode.scHash)
        candidates.add(mode.tid)

        val keys = listOf(
            mode.name,
            mode.scHash,
            mode.tid
        ).map {
            normalize(it).replace(" ", "")
        }

        modeAliases.forEach { (canonical, aliases) ->

            val normalizedCanonical =
                normalize(canonical).replace(" ", "")

            if (keys.any {
                it == normalizedCanonical ||
                    it.contains(normalizedCanonical)
            }) {
                candidates.add(canonical)
                candidates.addAll(aliases)
            }
        }

        return candidates
            .filter { it.isNotBlank() }
            .distinctBy { normalize(it) }
    }

    private fun detectMode(
        text: String,
        modes: List<BrawlGameMode>
    ): BrawlGameMode? {

        if (text.isBlank() || modes.isEmpty()) {
            return null
        }

        return modes
            .filter { !it.disabled }
            .map { mode ->

                val score = getModeCandidates(mode)
                    .maxOfOrNull { candidate ->
                        phraseScore(text, candidate)
                    } ?: 0.0

                mode to score
            }
            .filter { it.second > 0.0 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun detectMap(
        text: String,
        maps: List<BrawlMap>,
        modeId: Int
    ): BrawlMap? {

        if (modeId <= 0 || text.isBlank()) {
            return null
        }

        val compatibleMaps = maps.filter { map ->
            map.gameModeId == modeId &&
                map.name.isNotBlank() &&
                !map.disabled
        }

        if (compatibleMaps.isEmpty()) {
            return null
        }

        return compatibleMaps
            .map { map ->

                val score = phraseScore(
                    text,
                    map.name
                )

                map to score
            }
            .filter { it.second > 0.0 }
            .maxByOrNull { it.second }
            ?.first
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
            .addOnSuccessListener { visionText: Text ->

                val rawText = visionText.text

                val modes = BrawlModeMapStore.modes
                val maps = BrawlModeMapStore.maps

                // 1. Recherche du mode dans le catalogue BrawlAPI.
                val detectedMode = detectMode(
                    rawText,
                    modes
                )

                // 2. Recherche de la carte compatible.
                val detectedMap = detectedMode?.let { mode ->
                    detectMap(
                        rawText,
                        maps,
                        mode.id
                    )
                }

                val result = BrawlModeMapDetection(
                    modeName = detectedMode?.name,
                    mapName = detectedMap?.name,
                    detectedText = normalize(rawText)
                )

                // Conservation du dernier résultat.
                BrawlModeMapStore.updateDetection(result)

                onResult(Result.success(result))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun close() {
        recognizer.close()
    }
}

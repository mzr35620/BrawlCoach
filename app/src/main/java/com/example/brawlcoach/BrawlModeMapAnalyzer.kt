
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

                val detectedMap = BrawlModeMapStore.maps
                    .filter { it.name.isNotBlank() && !it.disabled }
                    .firstOrNull { map ->
                        val name = normalize(map.name)

                        name.isNotBlank() &&
                            detectedText.contains(name)
                    }

                val detectedModeFromText =
                    BrawlModeMapStore.modes
                        .filter { it.name.isNotBlank() }
                        .firstOrNull { mode ->
                            val name = normalize(mode.name)

                            name.isNotBlank() &&
                                detectedText.contains(name)
                        }

                // Si le mode n'est pas reconnu par OCR,
                // on essaie de le retrouver grâce à la carte.
                val detectedModeFromMap =
                    detectedMap?.gameMode?.let { mapMode ->

                        val normalizedMapMode = normalize(mapMode)

                        BrawlModeMapStore.modes.firstOrNull { mode ->
                            normalize(mode.name) == normalizedMapMode
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

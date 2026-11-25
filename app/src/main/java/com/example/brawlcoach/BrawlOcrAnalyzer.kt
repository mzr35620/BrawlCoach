
package com.example.brawlcoach

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.Normalizer
import java.util.Locale

object BrawlOcrAnalyzer {

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
        onResult: (Result<List<String>>) -> Unit
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

                val references = BrawlerReferenceDatabase.brawlers

                val recognizedNames = mutableSetOf<String>()

                for (block in visionText.textBlocks) {

                    val texts = block.lines.map {
                        normalize(it.text)
                    }.filter {
                        it.isNotBlank()
                    } + normalize(block.text)

                    for (brawler in references) {

                        val names = listOf(brawler.name) +
                            brawler.aliases

                        val found = names.any { candidate ->

                            val name = normalize(candidate)

                            if (name.isBlank()) {
                                false
                            } else {
                                texts.any { text ->
                                    text == name ||
                                    Regex(
                                        "(^| )${Regex.escape(name)}( |$)"
                                    ).containsMatchIn(text)
                                }
                            }
                        }

                        if (found) {
                            recognizedNames.add(brawler.name)
                        }
                    }
                }

                onResult(
                    Result.success(recognizedNames.toList())
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

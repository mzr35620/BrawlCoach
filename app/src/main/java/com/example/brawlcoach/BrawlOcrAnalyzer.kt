
package com.example.brawlcoach

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

object BrawlOcrAnalyzer {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    private fun normalize(value: String): String {
        return value.lowercase(Locale.ROOT)
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

                val recognizedNames = visionText.textBlocks
                    .flatMap { it.lines }
                    .map { line -> normalize(line.text) }
                    .filter { it.isNotBlank() }
                    .flatMap { text ->

                        references.filter { brawler ->

                            val names = listOf(brawler.name) +
                                brawler.aliases

                            names.any { candidate ->
                                val name = normalize(candidate)

                                name.isNotBlank() &&
                                    text == name
                            }
                        }.map { it.name }
                    }
                    .distinct()

                onResult(Result.success(recognizedNames))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun close() {
        recognizer.close()
    }
}

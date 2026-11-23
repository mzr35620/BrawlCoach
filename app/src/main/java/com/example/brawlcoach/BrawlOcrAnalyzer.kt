
package com.example.brawlcoach

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object BrawlOcrAnalyzer {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    /**
     * Analyse localement une image et conserve uniquement
     * les noms de Brawlers reconnus dans notre base.
     *
     * Aucun texte OCR n'est sauvegardé ou transmis.
     */
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

                val recognizedNames = visionText.textBlocks
                    .flatMap { it.lines }
                    .mapNotNull { line ->
                        BrawlerReferenceDatabase.find(line.text)?.name
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

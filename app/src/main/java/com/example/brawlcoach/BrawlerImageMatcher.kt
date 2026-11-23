
package com.example.brawlcoach

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

data class BrawlerMatch(
    val name: String,
    val confidence: Float
)

object BrawlerImageMatcher {

    private const val SAMPLE_SIZE = 32

    /**
     * Compare deux portraits et calcule leur ressemblance.
     */
    fun compare(
        captured: Bitmap,
        reference: Bitmap
    ): Float {

        if (captured.isRecycled || reference.isRecycled ||
            captured.width == 0 || captured.height == 0 ||
            reference.width == 0 || reference.height == 0
        ) {
            return 0f
        }

        var difference = 0.0
        var count = 0

        // Recadrage carré au centre des deux images.
        val capturedSide = minOf(captured.width, captured.height)
        val referenceSide = minOf(reference.width, reference.height)

        val capturedLeft = (captured.width - capturedSide) / 2
        val capturedTop = (captured.height - capturedSide) / 2

        val referenceLeft = (reference.width - referenceSide) / 2
        val referenceTop = (reference.height - referenceSide) / 2

        for (y in 0 until SAMPLE_SIZE) {
            for (x in 0 until SAMPLE_SIZE) {

                val cx = capturedLeft +
                    x * capturedSide / SAMPLE_SIZE
                val cy = capturedTop +
                    y * capturedSide / SAMPLE_SIZE

                val rx = referenceLeft +
                    x * referenceSide / SAMPLE_SIZE
                val ry = referenceTop +
                    y * referenceSide / SAMPLE_SIZE

                val color1 = captured.getPixel(cx, cy)
                val color2 = reference.getPixel(rx, ry)

                difference +=
                    abs(Color.red(color1) - Color.red(color2)) +
                    abs(Color.green(color1) - Color.green(color2)) +
                    abs(Color.blue(color1) - Color.blue(color2))

                count++
            }
        }

        val maximumDifference = count * 3.0 * 255.0

        return (1.0 - difference / maximumDifference)
            .toFloat()
            .coerceIn(0f, 1f)
    }

    /**
     * Recherche le portrait de référence le plus ressemblant.
     * Retourne null si le seuil n'est pas atteint.
     */
    fun findBestMatch(
        capturedPortrait: Bitmap,
        minimumConfidence: Float = 0.90f
    ): BrawlerMatch? {

        if (capturedPortrait.isRecycled) {
            return null
        }

        val references = BrawlerImageStore.getImages()

        if (references.isEmpty()) {
            return null
        }

        var bestMatch: BrawlerMatch? = null

        for ((name, reference) in references) {

            if (reference.isRecycled) {
                continue
            }

            val score = compare(capturedPortrait, reference)

            if (bestMatch == null ||
                score > bestMatch.confidence
            ) {
                bestMatch = BrawlerMatch(
                    name = name,
                    confidence = score
                )
            }
        }

        val result = bestMatch ?: return null

        return if (result.confidence >= minimumConfidence) {
            result
        } else {
            null
        }
    }
}


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

    fun compare(
        captured: Bitmap,
        reference: Bitmap
    ): Float {

        if (captured.isRecycled || reference.isRecycled ||
            captured.width <= 0 || captured.height <= 0 ||
            reference.width <= 0 || reference.height <= 0
        ) {
            return 0f
        }

        val capturedScaled = Bitmap.createScaledBitmap(
            captured,
            SAMPLE_SIZE,
            SAMPLE_SIZE,
            true
        )

        val referenceScaled = Bitmap.createScaledBitmap(
            reference,
            SAMPLE_SIZE,
            SAMPLE_SIZE,
            true
        )

        try {
            var difference = 0.0
            var count = 0

            for (y in 0 until SAMPLE_SIZE) {
                for (x in 0 until SAMPLE_SIZE) {

                    val color1 = capturedScaled.getPixel(x, y)
                    val color2 = referenceScaled.getPixel(x, y)

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

        } finally {
            capturedScaled.recycle()
            referenceScaled.recycle()
        }
    }

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

            if (reference.isRecycled) continue

            val score = compare(capturedPortrait, reference)

            if (bestMatch == null ||
                score > bestMatch.confidence
            ) {
                bestMatch = BrawlerMatch(name, score)
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

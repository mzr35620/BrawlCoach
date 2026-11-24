
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
            val capturedPixels = IntArray(
                SAMPLE_SIZE * SAMPLE_SIZE
            )

            val referencePixels = IntArray(
                SAMPLE_SIZE * SAMPLE_SIZE
            )

            capturedScaled.getPixels(
                capturedPixels, 0, SAMPLE_SIZE,
                0, 0, SAMPLE_SIZE, SAMPLE_SIZE
            )

            referenceScaled.getPixels(
                referencePixels, 0, SAMPLE_SIZE,
                0, 0, SAMPLE_SIZE, SAMPLE_SIZE
            )

            var colorDifference = 0.0
            var edgeDifference = 0.0
            var colorCount = 0
            var edgeCount = 0

            for (y in 0 until SAMPLE_SIZE) {
                for (x in 0 until SAMPLE_SIZE) {

                    val index = y * SAMPLE_SIZE + x

                    val color1 = capturedPixels[index]
                    val color2 = referencePixels[index]

                    val redDifference =
                        abs(Color.red(color1) - Color.red(color2))

                    val greenDifference =
                        abs(Color.green(color1) - Color.green(color2))

                    val blueDifference =
                        abs(Color.blue(color1) - Color.blue(color2))

                    colorDifference +=
                        redDifference + greenDifference + blueDifference

                    colorCount++

                    if (x < SAMPLE_SIZE - 1 &&
                        y < SAMPLE_SIZE - 1
                    ) {
                        val right = index + 1
                        val bottom = index + SAMPLE_SIZE

                        val gradient1 =
                            abs(
                                Color.red(capturedPixels[right]) -
                                Color.red(color1)
                            ) +
                            abs(
                                Color.green(capturedPixels[right]) -
                                Color.green(color1)
                            ) +
                            abs(
                                Color.blue(capturedPixels[right]) -
                                Color.blue(color1)
                            ) +
                            abs(
                                Color.red(capturedPixels[bottom]) -
                                Color.red(color1)
                            ) +
                            abs(
                                Color.green(capturedPixels[bottom]) -
                                Color.green(color1)
                            ) +
                            abs(
                                Color.blue(capturedPixels[bottom]) -
                                Color.blue(color1)
                            )

                        val gradient2 =
                            abs(
                                Color.red(referencePixels[right]) -
                                Color.red(color2)
                            ) +
                            abs(
                                Color.green(referencePixels[right]) -
                                Color.green(color2)
                            ) +
                            abs(
                                Color.blue(referencePixels[right]) -
                                Color.blue(color2)
                            ) +
                            abs(
                                Color.red(referencePixels[bottom]) -
                                Color.red(color2)
                            ) +
                            abs(
                                Color.green(referencePixels[bottom]) -
                                Color.green(color2)
                            ) +
                            abs(
                                Color.blue(referencePixels[bottom]) -
                                Color.blue(color2)
                            )

                        edgeDifference += abs(gradient1 - gradient2)
                        edgeCount++
                    }
                }
            }

            val colorScore =
                1.0 - colorDifference /
                (colorCount * 3.0 * 255.0)

            val edgeScore =
                1.0 - edgeDifference /
                (edgeCount * 6.0 * 255.0)

            return (
                colorScore * 0.65 +
                edgeScore * 0.35
            ).toFloat().coerceIn(0f, 1f)

        } finally {
            if (capturedScaled !== captured &&
                !capturedScaled.isRecycled
            ) {
                capturedScaled.recycle()
            }

            if (referenceScaled !== reference &&
                referenceScaled !== capturedScaled &&
                !referenceScaled.isRecycled
            ) {
                referenceScaled.recycle()
            }
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

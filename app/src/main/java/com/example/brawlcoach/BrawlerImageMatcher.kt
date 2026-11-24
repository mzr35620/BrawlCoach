
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

    private fun prepareBitmap(source: Bitmap): Bitmap? {

        if (source.isRecycled ||
            source.width <= 0 ||
            source.height <= 0
        ) {
            return null
        }

        var square: Bitmap? = null

        return try {
            val side = minOf(source.width, source.height)

            val left = (source.width - side) / 2
            val top = (source.height - side) / 2

            square = Bitmap.createBitmap(
                source,
                left,
                top,
                side,
                side
            )

            val scaled = Bitmap.createScaledBitmap(
                square,
                SAMPLE_SIZE,
                SAMPLE_SIZE,
                true
            )

            // Garantit un bitmap indépendant avant de recycler square.
            if (scaled === square) {
                Bitmap.createBitmap(scaled)
            } else {
                scaled
            }

        } catch (e: Exception) {
            null

        } finally {
            square?.let {
                if (it !== source && !it.isRecycled) {
                    it.recycle()
                }
            }
        }
    }

    fun compare(
        captured: Bitmap,
        reference: Bitmap
    ): Float {

        val capturedScaled = prepareBitmap(captured)
            ?: return 0f

        val referenceScaled = prepareBitmap(reference)
            ?: run {
                if (!capturedScaled.isRecycled) {
                    capturedScaled.recycle()
                }
                return 0f
            }

        try {
            val pixelCount = SAMPLE_SIZE * SAMPLE_SIZE

            val capturedPixels = IntArray(pixelCount)
            val referencePixels = IntArray(pixelCount)

            capturedScaled.getPixels(
                capturedPixels,
                0,
                SAMPLE_SIZE,
                0,
                0,
                SAMPLE_SIZE,
                SAMPLE_SIZE
            )

            referenceScaled.getPixels(
                referencePixels,
                0,
                SAMPLE_SIZE,
                0,
                0,
                SAMPLE_SIZE,
                SAMPLE_SIZE
            )

            var colorDifference = 0.0
            var edgeDifference = 0.0
            var edgeCount = 0

            for (y in 0 until SAMPLE_SIZE) {
                for (x in 0 until SAMPLE_SIZE) {

                    val index = y * SAMPLE_SIZE + x

                    val capturedColor = capturedPixels[index]
                    val referenceColor = referencePixels[index]

                    colorDifference +=
                        abs(Color.red(capturedColor) -
                            Color.red(referenceColor)) +
                        abs(Color.green(capturedColor) -
                            Color.green(referenceColor)) +
                        abs(Color.blue(capturedColor) -
                            Color.blue(referenceColor))

                    if (x < SAMPLE_SIZE - 1 &&
                        y < SAMPLE_SIZE - 1
                    ) {
                        val right = index + 1
                        val bottom = index + SAMPLE_SIZE

                        val capturedGradient = gradient(
                            capturedPixels[index],
                            capturedPixels[right],
                            capturedPixels[bottom]
                        )

                        val referenceGradient = gradient(
                            referencePixels[index],
                            referencePixels[right],
                            referencePixels[bottom]
                        )

                        edgeDifference += abs(
                            capturedGradient - referenceGradient
                        )

                        edgeCount++
                    }
                }
            }

            val colorScore =
                1.0 - colorDifference /
                (pixelCount * 3.0 * 255.0)

            val edgeScore =
                if (edgeCount > 0) {
                    1.0 - edgeDifference /
                    (edgeCount * 6.0 * 255.0)
                } else {
                    0.0
                }

            return (
                colorScore * 0.65 +
                edgeScore * 0.35
            ).toFloat().coerceIn(0f, 1f)

        } catch (e: Exception) {
            return 0f

        } finally {
            if (!capturedScaled.isRecycled) {
                capturedScaled.recycle()
            }

            if (!referenceScaled.isRecycled) {
                referenceScaled.recycle()
            }
        }
    }

    private fun gradient(
        center: Int,
        right: Int,
        bottom: Int
    ): Double {

        return (
            abs(Color.red(right) - Color.red(center)) +
            abs(Color.green(right) - Color.green(center)) +
            abs(Color.blue(right) - Color.blue(center)) +
            abs(Color.red(bottom) - Color.red(center)) +
            abs(Color.green(bottom) - Color.green(center)) +
            abs(Color.blue(bottom) - Color.blue(center))
        ).toDouble()
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

            if (reference.isRecycled ||
                reference.width <= 0 ||
                reference.height <= 0
            ) {
                continue
            }

            val score = compare(
                capturedPortrait,
                reference
            )

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

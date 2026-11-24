
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

        val side = minOf(source.width, source.height)

        val left = (source.width - side) / 2
        val top = (source.height - side) / 2

        val square = Bitmap.createBitmap(
            source,
            left,
            top,
            side,
            side
        )

        return Bitmap.createScaledBitmap(
            square,
            SAMPLE_SIZE,
            SAMPLE_SIZE,
            true
        ).also {
            if (square !== source && square !== it &&
                !square.isRecycled
            ) {
                square.recycle()
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
            val capturedPixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
            val referencePixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)

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
            var edgeCount = 0

            for (y in 0 until SAMPLE_SIZE) {
                for (x in 0 until SAMPLE_SIZE) {

                    val index = y * SAMPLE_SIZE + x

                    val c1 = capturedPixels[index]
                    val c2 = referencePixels[index]

                    colorDifference +=
                        abs(Color.red(c1) - Color.red(c2)) +
                        abs(Color.green(c1) - Color.green(c2)) +
                        abs(Color.blue(c1) - Color.blue(c2))

                    if (x < SAMPLE_SIZE - 1 &&
                        y < SAMPLE_SIZE - 1
                    ) {
                        val right = index + 1
                        val bottom = index + SAMPLE_SIZE

                        val gradient1 =
                            abs(Color.red(capturedPixels[right]) - Color.red(c1)) +
                            abs(Color.green(capturedPixels[right]) - Color.green(c1)) +
                            abs(Color.blue(capturedPixels[right]) - Color.blue(c1)) +
                            abs(Color.red(capturedPixels[bottom]) - Color.red(c1)) +
                            abs(Color.green(capturedPixels[bottom]) - Color.green(c1)) +
                            abs(Color.blue(capturedPixels[bottom]) - Color.blue(c1))

                        val gradient2 =
                            abs(Color.red(referencePixels[right]) - Color.red(c2)) +
                            abs(Color.green(referencePixels[right]) - Color.green(c2)) +
                            abs(Color.blue(referencePixels[right]) - Color.blue(c2)) +
                            abs(Color.red(referencePixels[bottom]) - Color.red(c2)) +
                            abs(Color.green(referencePixels[bottom]) - Color.green(c2)) +
                            abs(Color.blue(referencePixels[bottom]) - Color.blue(c2))

                        edgeDifference += abs(gradient1 - gradient2)
                        edgeCount++
                    }
                }
            }

            val colorScore =
                1.0 - colorDifference /
                (SAMPLE_SIZE * SAMPLE_SIZE * 3.0 * 255.0)

            val edgeScore =
                1.0 - edgeDifference /
                (edgeCount * 6.0 * 255.0)

            return (
                colorScore * 0.65 +
                edgeScore * 0.35
            ).toFloat().coerceIn(0f, 1f)

        } finally {
            if (!capturedScaled.isRecycled) {
                capturedScaled.recycle()
            }

            if (!referenceScaled.isRecycled) {
                referenceScaled.recycle()
            }
        }
    }

    fun findBestMatch(
        capturedPortrait: Bitmap,
        minimumConfidence: Float = 0.90f
    ): BrawlerMatch? {

        if (capturedPortrait.isRecycled) return null

        val references = BrawlerImageStore.getImages()

        if (references.isEmpty()) return null

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

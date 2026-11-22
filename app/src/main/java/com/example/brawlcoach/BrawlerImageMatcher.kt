package com.example.brawlcoach

import android.graphics.Bitmap
import kotlin.math.abs

data class BrawlerMatch(
    val name: String,
    val confidence: Float
)

object BrawlerImageMatcher {

    fun compare(
        captured: Bitmap,
        reference: Bitmap
    ): Float {

        if (captured.width == 0 || captured.height == 0 ||
            reference.width == 0 || reference.height == 0
        ) {
            return 0f
        }

        val sampleSize = 32
        var difference = 0.0
        var count = 0

        for (y in 0 until sampleSize) {
            for (x in 0 until sampleSize) {

                val cx = x * captured.width / sampleSize
                val cy = y * captured.height / sampleSize

                val rx = x * reference.width / sampleSize
                val ry = y * reference.height / sampleSize

                val color1 = captured.getPixel(cx, cy)
                val color2 = reference.getPixel(rx, ry)

                difference +=
                    abs(android.graphics.Color.red(color1) -
                        android.graphics.Color.red(color2)) +
                    abs(android.graphics.Color.green(color1) -
                        android.graphics.Color.green(color2)) +
                    abs(android.graphics.Color.blue(color1) -
                        android.graphics.Color.blue(color2))

                count++
            }
        }

        val maximumDifference = count * 3.0 * 255.0

        return (1.0 - difference / maximumDifference)
            .toFloat()
            .coerceIn(0f, 1f)
    }
}

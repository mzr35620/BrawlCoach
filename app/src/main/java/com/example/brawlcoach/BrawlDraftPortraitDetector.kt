
package com.example.brawlcoach

import android.graphics.Bitmap

data class DraftPortraitCrop(
    val slot: String,
    val team: String,
    val bitmap: Bitmap
)

object BrawlDraftPortraitDetector {

    private data class Region(
        val slot: String,
        val team: String,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    // Coordonnées proportionnelles à la capture
    // de sélection en classé fournie.
    private val regions = listOf(
        Region("Allié 1", "Alliés", 0.061f, 0.153f, 0.153f, 0.327f),
        Region("Allié 2", "Alliés", 0.061f, 0.333f, 0.153f, 0.495f),
        Region("Allié 3", "Alliés", 0.066f, 0.497f, 0.154f, 0.671f),

        Region("Adversaire 1", "Adversaires", 0.096f, 0.735f, 0.168f, 0.877f),
        Region("Adversaire 2", "Adversaires", 0.182f, 0.735f, 0.253f, 0.877f),
        Region("Adversaire 3", "Adversaires", 0.268f, 0.735f, 0.339f, 0.877f)
    )

    fun extract(bitmap: Bitmap): List<DraftPortraitCrop> {

        if (bitmap.isRecycled ||
            bitmap.width <= 0 ||
            bitmap.height <= 0
        ) {
            return emptyList()
        }

        val ratio = bitmap.width.toFloat() / bitmap.height

        // Évite d'utiliser ces coordonnées sur un écran
        // dont la disposition est trop différente.
        if (ratio !in 1.7f..2.2f) {
            return emptyList()
        }

        return regions.mapNotNull { region ->

            val left = (region.left * bitmap.width)
                .toInt().coerceIn(0, bitmap.width - 1)

            val top = (region.top * bitmap.height)
                .toInt().coerceIn(0, bitmap.height - 1)

            val right = (region.right * bitmap.width)
                .toInt().coerceIn(left + 1, bitmap.width)

            val bottom = (region.bottom * bitmap.height)
                .toInt().coerceIn(top + 1, bitmap.height)

            val crop = Bitmap.createBitmap(
                bitmap,
                left,
                top,
                right - left,
                bottom - top
            )

            DraftPortraitCrop(
                slot = region.slot,
                team = region.team,
                bitmap = crop
            )
        }
    }
}

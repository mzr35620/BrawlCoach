
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

    private const val MIN_WIDTH = 640
    private const val MIN_HEIGHT = 360

    // Coordonnées provisoires : à valider sur une capture
    // réelle de sélection en classé.
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
            bitmap.width < MIN_WIDTH ||
            bitmap.height < MIN_HEIGHT
        ) {
            return emptyList()
        }

        val ratio =
            bitmap.width.toFloat() / bitmap.height.toFloat()

        // La disposition actuelle est prévue pour une capture horizontale.
        if (ratio !in 1.7f..2.2f) {
            return emptyList()
        }

        val results = mutableListOf<DraftPortraitCrop>()

        for (region in regions) {

            if (region.left < 0f ||
                region.top < 0f ||
                region.right > 1f ||
                region.bottom > 1f ||
                region.left >= region.right ||
                region.top >= region.bottom
            ) {
                continue
            }

            val left = (region.left * bitmap.width).toInt()
            val top = (region.top * bitmap.height).toInt()
            val right = (region.right * bitmap.width).toInt()
            val bottom = (region.bottom * bitmap.height).toInt()

            if (left < 0 ||
                top < 0 ||
                right > bitmap.width ||
                bottom > bitmap.height ||
                right <= left ||
                bottom <= top
            ) {
                continue
            }

            try {
                val crop = Bitmap.createBitmap(
                    bitmap,
                    left,
                    top,
                    right - left,
                    bottom - top
                )

                results.add(
                    DraftPortraitCrop(
                        slot = region.slot,
                        team = region.team,
                        bitmap = crop
                    )
                )

            } catch (e: IllegalArgumentException) {
                // Ignore uniquement la zone invalide.
            }
        }

        return results
    }
}

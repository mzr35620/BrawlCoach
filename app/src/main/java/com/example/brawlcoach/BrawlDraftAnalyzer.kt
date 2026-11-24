
package com.example.brawlcoach

import android.graphics.Bitmap

data class DraftSlotResult(
    val slot: String,
    val team: String,
    val brawlerName: String,
    val confidence: Float
)

object BrawlDraftAnalyzer {

    fun analyze(bitmap: Bitmap): List<DraftSlotResult> {

        if (bitmap.isRecycled) {
            return emptyList()
        }

        val crops = BrawlDraftPortraitDetector.extract(bitmap)

        return crops.map { crop ->

            try {
                // Les choix adverses masqués restent inconnus.
                if (crop.team == "Adversaires") {
                    DraftSlotResult(
                        slot = crop.slot,
                        team = crop.team,
                        brawlerName = "Inconnu",
                        confidence = 0f
                    )
                } else {
                    val match = BrawlerImageMatcher.findBestMatch(
                        capturedPortrait = crop.bitmap,
                        minimumConfidence = 0.90f
                    )

                    DraftSlotResult(
                        slot = crop.slot,
                        team = crop.team,
                        brawlerName = match?.name ?: "Inconnu",
                        confidence = match?.confidence ?: 0f
                    )
                }
            } finally {
                if (!crop.bitmap.isRecycled) {
                    crop.bitmap.recycle()
                }
            }
        }
    }
}

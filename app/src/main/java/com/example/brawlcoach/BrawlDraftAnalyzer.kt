
package com.example.brawlcoach

import android.graphics.Bitmap

data class DraftSlotResult(
    val slot: String,
    val team: String,
    val brawlerName: String,
    val confidence: Float,
    val bestCandidateName: String? = null
)

object BrawlDraftAnalyzer {

    private const val MINIMUM_CONFIDENCE = 0.90f

    fun analyze(bitmap: Bitmap): List<DraftSlotResult> {

        if (bitmap.isRecycled ||
            bitmap.width <= 0 ||
            bitmap.height <= 0
        ) {
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
                        confidence = 0f,
                        bestCandidateName = null
                    )

                } else {

                    // Récupère le meilleur candidat,
                    // même si son score est inférieur à 90 %.
                    val match = BrawlerImageMatcher.findBestMatch(
                        capturedPortrait = crop.bitmap,
                        minimumConfidence = 0f
                    )

                    val score = match?.confidence ?: 0f

                    DraftSlotResult(
                        slot = crop.slot,
                        team = crop.team,
                        brawlerName = if (
                            match != null &&
                            score >= MINIMUM_CONFIDENCE
                        ) {
                            match.name
                        } else {
                            "Inconnu"
                        },
                        confidence = score,
                        bestCandidateName = match?.name
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

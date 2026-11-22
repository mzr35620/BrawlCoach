package com.example.brawlcoach

data class BrawlerRecognition(
    val name: String,
    val confidence: Float
)

object BrawlerRecognitionEngine {

    fun recognize(): List<BrawlerRecognition> {
        // Reconnaissance à implémenter.
        // Aucune image n'est capturée ou analysée ici.
        return emptyList()
    }
}

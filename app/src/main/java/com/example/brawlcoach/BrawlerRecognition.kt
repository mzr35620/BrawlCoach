
package com.example.brawlcoach

data class BrawlerRecognition(
    val name: String,
    val confidence: Float
)

object BrawlerRecognitionEngine {

    // À utiliser lorsque le système OCR aura détecté des noms.
    fun recognizeDetectedNames(
        detectedNames: List<Pair<String, Float>>
    ): List<BrawlerRecognition> {

        return detectedNames.mapNotNull { detected ->

            val reference = BrawlerReferenceDatabase.find(
                detected.first
            )

            reference?.let {
                BrawlerRecognition(
                    name = it.name,
                    confidence = detected.second.coerceIn(0f, 1f)
                )
            }
        }.distinctBy { it.name }
    }

    // Compatibilité avec l'ancienne fonction.
    fun recognize(): List<BrawlerRecognition> {
        return emptyList()
    }
}

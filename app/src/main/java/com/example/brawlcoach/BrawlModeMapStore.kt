package com.example.brawlcoach

data class BrawlModeMapDetection(
    val modeName: String?,
    val mapName: String?,
    val detectedText: String
)

object BrawlModeMapStore {

    @Volatile
    var modes: List<BrawlGameMode> = emptyList()

    @Volatile
    var maps: List<BrawlMap> = emptyList()

    @Volatile
    var latestDetection: BrawlModeMapDetection? = null

    fun setReferences(
        newModes: List<BrawlGameMode>,
        newMaps: List<BrawlMap>
    ) {
        modes = newModes
        maps = newMaps
    }

    fun updateDetection(
        detection: BrawlModeMapDetection
    ) {
        latestDetection = detection
    }

    fun clearDetection() {
        latestDetection = null
    }
}

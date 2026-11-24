package com.example.brawlcoach

object BrawlDraftResultStore {

    @Volatile
    private var latestResults: List<DraftSlotResult> = emptyList()

    fun update(results: List<DraftSlotResult>) {
        latestResults = results.toList()
    }

    fun getResults(): List<DraftSlotResult> {
        return latestResults
    }

    fun clear() {
        latestResults = emptyList()
    }
}

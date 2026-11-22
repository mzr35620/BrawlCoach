
package com.example.brawlcoach

data class BrawlerStats(
    val name: String,
    val winRate: Double,
    val pickRate: Double,
    val className: String = "",
    val synergyScore: Double = 0.0,
    val counterScore: Double = 0.0
)

data class BrawlerRecommendation(
    val name: String,
    val score: Double,
    val winRate: Double,
    val pickRate: Double,
    val className: String
)

object BrawlRecommendationEngine {

    fun recommend(
        stats: List<BrawlerStats>,
        teamPicks: List<String>,
        enemyPicks: List<String>,
        limit: Int = 3
    ): List<BrawlerRecommendation> {

        if (stats.isEmpty()) return emptyList()

        val unavailable = (teamPicks + enemyPicks)
            .map { it.lowercase().trim() }
            .toSet()

        return stats
            .filter {
                it.name.lowercase().trim() !in unavailable
            }
            .filter {
                it.winRate in 0.0..100.0 &&
                it.pickRate in 0.0..100.0
            }
            .map { brawler ->

                val winScore = brawler.winRate
                val pickScore = brawler.pickRate

                val finalScore =
                    winScore * 0.65 +
                    pickScore * 0.10 +
                    brawler.synergyScore * 0.15 +
                    brawler.counterScore * 0.10

                BrawlerRecommendation(
                    name = brawler.name,
                    score = finalScore,
                    winRate = brawler.winRate,
                    pickRate = brawler.pickRate,
                    className = brawler.className
                )
            }
            .sortedByDescending { it.score }
            .take(limit.coerceAtLeast(1))
    }
}


package com.example.brawlcoach

data class BrawlerReference(
    val name: String,
    val aliases: List<String> = emptyList()
)

object BrawlerReferenceDatabase {

    @Volatile
    private var currentBrawlers = listOf(
        BrawlerReference("Shelly"),
        BrawlerReference("Colt"),
        BrawlerReference("Bull"),
        BrawlerReference("Brock"),
        BrawlerReference("Rico"),
        BrawlerReference("Spike"),
        BrawlerReference("Barley"),
        BrawlerReference("Jessie"),
        BrawlerReference("Nita"),
        BrawlerReference("Dynamike"),
        BrawlerReference("El Primo"),
        BrawlerReference("Mortis"),
        BrawlerReference("Crow"),
        BrawlerReference("Poco"),
        BrawlerReference("Bo"),
        BrawlerReference("Piper"),
        BrawlerReference("Pam"),
        BrawlerReference("Frank"),
        BrawlerReference("Bibi"),
        BrawlerReference("Leon"),
        BrawlerReference("Gene"),
        BrawlerReference("Max"),
        BrawlerReference("Mr. P"),
        BrawlerReference("Jacky"),
        BrawlerReference("Gale"),
        BrawlerReference("Nani"),
        BrawlerReference("Amber"),
        BrawlerReference("Colette"),
        BrawlerReference("Edgar"),
        BrawlerReference("Ruffs"),
        BrawlerReference("Buzz"),
        BrawlerReference("Fang"),
        BrawlerReference("Eve"),
        BrawlerReference("Janet"),
        BrawlerReference("Otis"),
        BrawlerReference("Sam"),
        BrawlerReference("Mandy"),
        BrawlerReference("Maisie"),
        BrawlerReference("Hank"),
        BrawlerReference("Pearl"),
        BrawlerReference("Larry & Lawrie"),
        BrawlerReference("Angelo"),
        BrawlerReference("Melodie"),
        BrawlerReference("Lily"),
        BrawlerReference("Clancy"),
        BrawlerReference("Moe"),
        BrawlerReference("Juju"),
        BrawlerReference("Ollie"),
        BrawlerReference("Lumi")
    )

    val brawlers: List<BrawlerReference>
        get() = currentBrawlers

    fun setBrawlers(names: List<String>) {
        if (names.isEmpty()) return

        currentBrawlers = names
            .distinctBy { it.trim().lowercase() }
            .map { BrawlerReference(it.trim()) }
    }

    fun find(name: String): BrawlerReference? {
        val search = name.trim()

        return currentBrawlers.firstOrNull {
            it.name.equals(search, ignoreCase = true) ||
            it.aliases.any { alias ->
                alias.equals(search, ignoreCase = true)
            }
        }
    }
}

package com.example.brawlcoach

data class BrawlerReference(
    val name: String,
    val aliases: List<String> = emptyList()
)

object BrawlerReferenceDatabase {

    val brawlers = listOf(
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

    fun find(name: String): BrawlerReference? {
        return brawlers.firstOrNull {
            it.name.equals(name.trim(), ignoreCase = true) ||
            it.aliases.any { alias ->
                alias.equals(name.trim(), ignoreCase = true)
            }
        }
    }
}

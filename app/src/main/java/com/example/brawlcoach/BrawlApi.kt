
package com.example.brawlcoach

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BrawlAbility(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val description: String
)

data class Brawler(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val rarity: String,
    val className: String,
    val gadgets: List<BrawlAbility>,
    val starPowers: List<BrawlAbility>
)

data class BrawlMap(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val gameMode: String,
    val disabled: Boolean
)

data class BrawlGameMode(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val disabled: Boolean
)

object BrawlApi {

    private const val BASE_URL = "https://api.brawlapi.com"

    private fun download(path: String): JSONObject {
        val connection = URL("$BASE_URL$path").openConnection()
            as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (connection.responseCode != 200) {
                throw Exception("Erreur API : ${connection.responseCode}")
            }

            val result = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            return JSONObject(result)
        } finally {
            connection.disconnect()
        }
    }

    private fun readAbilities(
        json: JSONObject,
        key: String
    ): List<BrawlAbility> {
        val array = json.optJSONArray(key)
            ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index)
                ?: return@mapNotNull null

            if (!item.optBoolean("released", true)) {
                return@mapNotNull null
            }

            BrawlAbility(
                id = item.optInt("id"),
                name = item.optString("name"),
                imageUrl = item.optString("imageUrl"),
                description = item.optString("description")
            )
        }
    }

    fun getBrawlers(): List<Brawler> {
        val array = download("/v1/brawlers")
            .optJSONArray("list")
            ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index)
                ?: return@mapNotNull null

            if (!item.optBoolean("released", true)) {
                return@mapNotNull null
            }

            val rarity = item.optJSONObject("rarity")
            val brawlerClass = item.optJSONObject("class")

            Brawler(
                id = item.optInt("id"),
                name = item.optString("name"),
                imageUrl = item.optString("imageUrl2"),
                rarity = rarity?.optString("name") ?: "",
                className = brawlerClass?.optString("name") ?: "",
                gadgets = readAbilities(item, "gadgets"),
                starPowers = readAbilities(item, "starPowers")
            )
        }
    }

    fun getMaps(): List<BrawlMap> {
        val array = download("/v1/maps")
            .optJSONArray("list")
            ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index)
                ?: return@mapNotNull null

            val mode = item.optJSONObject("gameMode")

            BrawlMap(
                id = item.optInt("id"),
                name = item.optString("name"),
                imageUrl = item.optString("imageUrl"),
                gameMode = mode?.optString("name") ?: "",
                disabled = item.optBoolean("disabled")
            )
        }
    }

    fun getGameModes(): List<BrawlGameMode> {
        val array = download("/v1/gamemodes")
            .optJSONArray("list")
            ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index)
                ?: return@mapNotNull null

            BrawlGameMode(
                id = item.optInt("id"),
                name = item.optString("name"),
                imageUrl = item.optString("imageUrl"),
                disabled = item.optBoolean("disabled")
            )
        }
    }
}

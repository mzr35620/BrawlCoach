
package com.example.brawlcoach

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object BrawlerImageRepository {

    fun downloadAll(
        brawlers: List<Brawler>
    ): Map<String, Bitmap> {

        val images = mutableMapOf<String, Bitmap>()
        val executor = Executors.newFixedThreadPool(4)

        try {
            val tasks = brawlers.mapNotNull { brawler ->

                val imageUrl = brawler.imageUrl.trim()

                if (imageUrl.isBlank()) {
                    null
                } else {
                    executor.submit<Pair<String, Bitmap?>?> {

                        var connection: HttpURLConnection? = null

                        try {
                            connection =
                                URL(imageUrl).openConnection()
                                    as HttpURLConnection

                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000
                            connection.requestMethod = "GET"

                            if (connection.responseCode != 200) {
                                return@submit null
                            }

                            val bitmap = connection.inputStream.use {
                                BitmapFactory.decodeStream(it)
                            }

                            if (bitmap != null) {
                                brawler.name to bitmap
                            } else {
                                null
                            }

                        } catch (e: Exception) {
                            null
                        } finally {
                            connection?.disconnect()
                        }
                    }
                }
            }

            for (task in tasks) {
                val result = task.get()
                if (result != null) {
                    images[result.first] = result.second
                }
            }

        } finally {
            executor.shutdownNow()
        }

        return images
    }
}

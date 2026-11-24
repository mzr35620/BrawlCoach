
package com.example.brawlcoach

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors

object BrawlerImageRepository {

    private const val CONNECT_TIMEOUT = 10000
    private const val READ_TIMEOUT = 10000
    private const val MAX_THREADS = 4

    fun downloadAll(
        brawlers: List<Brawler>
    ): Map<String, Bitmap> {

        if (brawlers.isEmpty()) {
            return emptyMap()
        }

        val images = mutableMapOf<String, Bitmap>()

        val executor = Executors.newFixedThreadPool(
            minOf(MAX_THREADS, brawlers.size)
        )

        try {
            val tasks = brawlers.mapNotNull { brawler ->

                val imageUrl = brawler.imageUrl.trim()
                val name = brawler.name.trim()

                if (imageUrl.isBlank() ||
                    name.isBlank()
                ) {
                    null
                } else {

                    executor.submit(
                        Callable<Pair<String, Bitmap?>?> {

                            var connection: HttpURLConnection? = null

                            try {
                                val url = URL(imageUrl)

                                if (url.protocol != "https" &&
                                    url.protocol != "http"
                                ) {
                                    return@Callable null
                                }

                                connection =
                                    url.openConnection()
                                        as HttpURLConnection

                                connection.connectTimeout =
                                    CONNECT_TIMEOUT

                                connection.readTimeout =
                                    READ_TIMEOUT

                                connection.requestMethod = "GET"
                                connection.instanceFollowRedirects = true

                                connection.setRequestProperty(
                                    "Accept",
                                    "image/*"
                                )

                                val responseCode =
                                    connection.responseCode

                                if (responseCode !in 200..299) {
                                    return@Callable null
                                }

                                val bitmap =
                                    connection.inputStream.use { stream ->
                                        BitmapFactory.decodeStream(stream)
                                    }

                                if (bitmap != null &&
                                    bitmap.width > 0 &&
                                    bitmap.height > 0 &&
                                    !bitmap.isRecycled
                                ) {
                                    name to bitmap
                                } else {
                                    bitmap?.recycle()
                                    null
                                }

                            } catch (e: Exception) {
                                null

                            } finally {
                                connection?.disconnect()
                            }
                        }
                    )
                }
            }

            for (task in tasks) {

                try {
                    val result = task.get()

                    if (result != null) {
                        val name = result.first
                        val bitmap = result.second

                        if (bitmap != null &&
                            !bitmap.isRecycled
                        ) {
                            images[name] = bitmap
                        }
                    }

                } catch (e: Exception) {
                    // Un téléchargement raté
                    // ne bloque pas les autres portraits.
                }
            }

        } finally {
            executor.shutdownNow()
        }

        return images
    }
}

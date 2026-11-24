
package com.example.brawlcoach

import android.graphics.Bitmap

object BrawlerImageStore {

    @Volatile
    private var images: Map<String, Bitmap> = emptyMap()

    @Synchronized
    fun setImages(newImages: Map<String, Bitmap>) {

        images = newImages.filter { (_, bitmap) ->
            !bitmap.isRecycled &&
            bitmap.width > 0 &&
            bitmap.height > 0
        }.toMap()
    }

    fun getImages(): Map<String, Bitmap> {
        return images.filter { (_, bitmap) ->
            !bitmap.isRecycled
        }
    }

    fun getImage(name: String): Bitmap? {
        val bitmap = images[name]

        return bitmap?.takeIf {
            !it.isRecycled &&
            it.width > 0 &&
            it.height > 0
        }
    }

    fun count(): Int {
        return getImages().size
    }

    @Synchronized
    fun clear() {
        images = emptyMap()
    }
}

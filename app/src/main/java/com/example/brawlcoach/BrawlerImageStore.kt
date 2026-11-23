
package com.example.brawlcoach

import android.graphics.Bitmap

object BrawlerImageStore {

    @Volatile
    private var images: Map<String, Bitmap> = emptyMap()

    fun setImages(newImages: Map<String, Bitmap>) {
        images = newImages.toMap()
    }

    fun getImages(): Map<String, Bitmap> {
        return images
    }

    fun getImage(name: String): Bitmap? {
        return images[name]
    }

    fun count(): Int {
        return images.size
    }
}

package models

import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories


object ImageStorage {

    fun loadImage(url: String) {
        try {
            URL(url).let {
                val stream = it.openStream()
                val path = Paths.get("images", it.file)
                path.parent.createDirectories()
                Files.copy(stream, path)
                storage[url] = path
            }
        } catch (e: IOException) {
            System.err.println("Failed to load image $url: $e")
        }
    }

    fun getImage(url: String) =
        storage[url]?.toFile()

    private val storage = ConcurrentHashMap<String, Path>()

}
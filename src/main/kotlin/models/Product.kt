package models

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Product(var name: String, var imageUrl: String, val id: String = computeNewId()) {
    init {
        ImageStorage.loadImage(imageUrl)
    }

    fun update(product: Product) {
        name = product.name
        if (imageUrl != product.imageUrl) {
            imageUrl = product.imageUrl
            ImageStorage.loadImage(imageUrl)
        }
    }
}

val productStorage = mutableListOf<Product>()

fun computeNewId() = UUID.randomUUID().toString()

package models

import kotlinx.serialization.Serializable
import java.util.*

typealias Id = String

@Serializable
data class Product(
    var name: String,
    val id: Id = computeNewId(),
    var onlyForRegistered: Boolean = false
) {
    fun update(product: Product) {
        name = product.name
        onlyForRegistered = product.onlyForRegistered
    }

    fun isAvailable(isRegistered: Boolean): Boolean {
        return !onlyForRegistered || isRegistered
    }

}

fun computeNewId(): Id = UUID.randomUUID().toString()

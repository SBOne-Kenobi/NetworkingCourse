package models

import kotlinx.serialization.Serializable

@Serializable
data class ProductWithToken(
    val product: Product,
    val token: String? = null
)

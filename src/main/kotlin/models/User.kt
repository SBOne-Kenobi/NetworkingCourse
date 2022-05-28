package models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.net.InetAddress

@Serializable
data class User(
    val email: String,
    val password: String
) {
    @Transient
    lateinit var ipAddress: InetAddress

    @Transient
    var token: Token? = null
}

@Serializable
data class Token(
    val token: String
)

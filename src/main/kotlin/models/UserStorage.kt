package models

import java.net.InetAddress
import java.util.*

object UserStorage {

    private val data = mutableMapOf<String, User>()
    private val dataByTokens = mutableMapOf<Token, String>()
    private val dataByIp = mutableMapOf<InetAddress, String>()

    fun createNewToken(email: String): Token {
        val user = data[email]!!
        if (user.token != null) return user.token!!
        val newToken = Token(UUID.randomUUID().toString())
        dataByTokens[newToken] = email
        user.token = newToken
        return newToken
    }

    fun registerUser(user: User): Boolean {
        return if (user.email in data) {
            false
        } else {
            data[user.email] = user
            dataByIp[user.ipAddress] = user.email
            true
        }
    }

    fun checkPassword(user: User): Boolean {
        return data[user.email]?.let {
            user.password == it.password
        } ?: false
    }

    fun getUserByToken(token: Token): User? {
        return dataByTokens[token]?.let { data[it] }
    }

    fun getUserByIpAddress(ipAddress: InetAddress): User? {
        return dataByIp[ipAddress]?.let { data[it] }
    }

}
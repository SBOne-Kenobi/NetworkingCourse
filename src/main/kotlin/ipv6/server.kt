package ipv6

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet6Address
import java.net.InetAddress
import java.net.ServerSocket

val Inet6Address.compressedAddress
    get() = hostAddress
        .replace("((?::0\\b){2,}):?(?!\\S*\\b\\1:0\\b)(\\S*)".toRegex(), "::$2")
        .replaceFirst("^0::".toRegex(),"::")

fun main() {
    val serverSocket = ServerSocket(
        Settings.serverPort,
        50,
        InetAddress.getByName(Settings.serverHost)
    )
    while (true) {
        val socket = serverSocket.accept()
        val output = PrintWriter(socket.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val message = input.readLine()
        val address = (socket.inetAddress as Inet6Address).compressedAddress
        println("Received from ${address}:${socket.port}: $message")
        val response = message.uppercase()
        println("Send to ${address}:${socket.port}: $response")
        output.println(response)
        socket.close()
    }

}
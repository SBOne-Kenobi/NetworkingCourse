package ipv6

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket

fun main() {
    val socket = Socket(
        InetAddress.getByName(Settings.serverHost),
        Settings.serverPort
    )
    val output = PrintWriter(socket.getOutputStream(), true)
    val input = BufferedReader(InputStreamReader(socket.getInputStream()))
    val message = "Message from client"
    println("Sending message: $message")
    output.println(message)
    val response = input.readLine()
    println("Response: $response")
    socket.close()
}
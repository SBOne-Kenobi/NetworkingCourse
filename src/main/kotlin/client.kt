import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

fun buildRequest(path: String): String =
    """GET / HTTP/1.1
Content-Type: text/html
Content-Length: ${path.length}

$path""".trimIndent()

fun client(host: String, port: Int, path: String) {
    val clientSocket = Socket(host, port)
    val output = PrintWriter(clientSocket.getOutputStream(), true)
    val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

    output.println(buildRequest(path))
    println(input.readText())

    clientSocket.close()
}

fun main(args: Array<String>) {
    client(args[0], args[1].toInt(), args[2])
}
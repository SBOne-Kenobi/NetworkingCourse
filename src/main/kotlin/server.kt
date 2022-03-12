import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

fun buildResponse(code: Int, message: String, body: String? = null) =
    if (body == null)
        """HTTP/1.1 $code $message

        """.trimIndent()
    else
        """HTTP/1.1 $code $message
Content-Type: text/html
Content-Length: ${body.length}

$body""".trimIndent()

fun parseRequest(input: BufferedReader): String? {
    input.readLine()
    val headers = hashMapOf<String, String>()
    var length = 0
    while (true) {
        val line = input.readLine()
        if (line.isBlank())
            break
        val idx = line.indexOfFirst { it == ':' }
        if (idx == -1)
            return null
        val name = line.take(idx)
        val value = line.drop(idx + 2)
        headers[name] = value
        if (name == "Content-Length") {
            length = value.toIntOrNull()
                ?: return null
        }
    }
    val body = CharArray(length)
    input.read(body)
    return String(body)
}

fun formResponseBody(path: String): String? {
    val file = File(path)
    return if (file.exists())
        file.readText()
    else
        null
}

fun serverRoutine(clientSocket: Socket) {
    val output = PrintWriter(clientSocket.getOutputStream(), true)
    val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

    val request = parseRequest(input)

    val response = if (request == null) {
        buildResponse(400, "Bad request", "")
    } else {
        formResponseBody(request)?.let {
            buildResponse(200, "OK", it)
        } ?: buildResponse(404, "Not found")
    }
    output.println(response)

    clientSocket.close()
}

fun server() {
    val serverPort = 8080
    val serverSocket = ServerSocket(serverPort)
    while (true) {
        val clientSocket = serverSocket.accept()
        Thread {
            serverRoutine(clientSocket)
        }.start()
    }
}

fun main() {
    server()
}
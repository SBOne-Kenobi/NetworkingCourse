import java.io.*
import java.net.ServerSocket

data class HttpRequest(val method: String, val headers: Map<String, String>, val body: String)

fun buildResponse(code: Int, message: String, body: String? = null) =
    if (body == null)
        """HTTP/1.1 $code $message

        """.trimIndent()
    else
        """HTTP/1.1 $code $message
Content-Type: text/html
Content-Length: ${body.length}

$body""".trimIndent()

fun parseRequest(input: BufferedReader): HttpRequest? {
    val startLine = input.readLine()
    val method = startLine.split(" ").getOrNull(0)
        ?: return null
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
    return HttpRequest(method, headers, String(body))
}

fun formResponseBody(request: HttpRequest): String? {
    val file = File(request.body)
    return if (file.exists())
        file.readText()
    else
        null
}

fun server() {
    val serverPort = 8080
    val serverSocket = ServerSocket(serverPort)
    while (true) {
        val clientSocket = serverSocket.accept()
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
}

fun main() {
    server()
}
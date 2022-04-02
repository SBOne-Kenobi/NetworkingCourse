import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Path

class FTPClient(
    host: String,
    port: Int,
    private val activeAddress: String? = "127.0.0.1",
    private val debugOutput: Boolean = false
) : AutoCloseable {

    private val socket = Socket(host, port)

    private val input = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val output = socket.getOutputStream()
    private val printerOutput = PrintWriter(output, true)

    private val activeServer = activeAddress?.let { ServerSocket(0) }

    private fun checkCode(expectedCode: Int) = { code: Int, message: String ->
        if (code != expectedCode) {
            throw RuntimeException("Expected $expectedCode, but received $code: $message")
        }
        message
    }

    private val noHandle = { _: Int, _: String -> }

    init {
        Thread.sleep(10)
        while (input.ready())
            response(noHandle)
    }


    private fun receiveFromServer(s: Socket, outputStream: OutputStream) {
        s.getInputStream().copyTo(outputStream)
        outputStream.flush()
        s.close()
    }

    private fun sendToServer(s: Socket, inputStream: InputStream) {
        val output = s.getOutputStream()
        inputStream.copyTo(output)
        output.flush()
        s.close()
    }

    private fun getSocketBuilder(): () -> Socket {
        send("TYPE I", checkCode(200))
        if (activeServer == null) {
            val message = send("PASV", checkCode(227))
            val addr = message
                .substringAfterLast(' ')
                .removeSurrounding("(", ")")
                .split(',')
            val host = addr.take(4).joinToString(".")
            val port = addr.drop(4).fold(0) { acc, s -> acc * 256 + s.toInt() }
            val activeSocket = Socket(host, port)
            return {
                activeSocket
            }
        } else {
            val addr = buildString {
                append(activeAddress!!.replace('.', ','))
                val port = activeServer.localPort
                append(",${port / 256},${port % 256}")
            }
            send("PORT $addr", checkCode(200))
            return {
                activeServer.accept()
            }
        }
    }

    fun <T> response(handler: (Int, String) -> T): T {
        val response = input.readLine()
        if (debugOutput) println("S: $response")
        val code = response.takeWhile { it.isDigit() }.toInt()
        val message = response.dropWhile { it.isDigit() }.trim()
        return handler(code, message)
    }

    fun send(message: String) {
        if (debugOutput) println("C: $message")
        printerOutput.println(message)
    }

    fun <T> send(message: String, handler: (Int, String) -> T): T {
        send(message)
        return response(handler)
    }

    fun login(login: String, password: String? = null) {
        send("USER $login", checkCode(331))
        send("PASS ${password ?: ""}", checkCode(230))
    }

    fun getCurrentDirectory(): String {
        return send("PWD", checkCode(257))
            .substringBefore(' ')
            .removeSurrounding("\"")
    }

    fun getList(): List<FileHierarchy> = runBlocking {
        val socketBuilder = getSocketBuilder()
        val result = async {
            val outputStream = ByteArrayOutputStream()
            receiveFromServer(socketBuilder(), outputStream)
            return@async String(outputStream.toByteArray())
        }
        send("LIST", checkCode(150))
        response(checkCode(226))
        result.await().trim().let {
            if (it.isBlank())
                emptyList()
            else
                it.lines().map { FileHierarchy.parse(it) }
        }
    }

    fun sendFile(path: Path) = runBlocking {
        val socketBuilder = getSocketBuilder()
        val job = launch {
            val file = path.toFile()
            sendToServer(socketBuilder(), file.inputStream())
        }
        send("STOR ${path.fileName}", checkCode(150))
        job.join()
        response(checkCode(226))
    }

    fun loadFile(fileName: String, outputStream: OutputStream) = runBlocking {
        val socketBuilder = getSocketBuilder()
        val result = launch {
            receiveFromServer(socketBuilder(), outputStream)
        }
        send("RETR $fileName", checkCode(150))
        response(checkCode(226))
        result.join()
    }

    fun goTo(folder: String) {
        send("CWD $folder", checkCode(250))
    }

    fun mkDir(folder: String) {
        send("MKD $folder", checkCode(257))
    }

    fun rmDir(folder: String) {
        send("RMD $folder", checkCode(250))
    }

    fun rmFile(fileName: String) {
        send("DELE $fileName", checkCode(250))
    }

    fun quit() {
        send("QUIT", checkCode(200))
    }

    override fun close() {
        socket.close()
    }
}
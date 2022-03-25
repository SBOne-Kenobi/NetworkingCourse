import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread

fun serverRoutine(clientSocket: Socket) {
    println("Client connected $clientSocket")
    val outputStream = clientSocket.getOutputStream()
    val output = PrintWriter(outputStream, true)
    val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
    output.println("Welcome!")

    val console = Runtime.getRuntime()
    while (true) {
        output.print("> ")
        output.flush()
        val request = input.readLine().trim()
        if (request == "exit") {
            break
        }
        val process = console.exec("cmd.exe /c $request")
        val err = thread {
            process.errorStream.copyTo(outputStream)
        }
        val out = thread {
            process.inputStream.copyTo(outputStream)
        }
        process.waitFor()
        err.join()
        out.join()
    }

    println("Client disconnected $clientSocket")
    clientSocket.close()
}

fun server(maxThreadsCount: Int) {
    val serverPort = 8080
    val serverSocket = ServerSocket(serverPort, maxThreadsCount)
    val threadPool = Executors.newFixedThreadPool(maxThreadsCount)
    println("Server starts")
    while (true) {
        val clientSocket = serverSocket.accept()
        threadPool.submit {
            serverRoutine(clientSocket)
        }
    }
}

fun main(args: Array<String>) {
    val threads = args.getOrNull(0)?.toIntOrNull() ?: 1
    server(threads)
}
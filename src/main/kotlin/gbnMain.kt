import kotlinx.coroutines.runBlocking
import protocol.GBNReceiver
import protocol.GBNSender
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.ceil

fun server(file: File) {
    file.writeBytes(ByteArray(0))
    val server = GBNServer(
        Settings.serverPort
    )
    try {
        runBlocking {
            server.receiveData().collect {
                State.updateServer(it)
                if (it is GBNReceiver.NewFrameEvent) {
                    it.frame.data?.let { data -> file.appendBytes(data) }
                }
            }
        }
    } catch (_: InterruptedException) {}
}

fun client(file: File) {
    val client = GBNClient(
        Settings.serverHost,
        Settings.serverPort,
        Settings.windowSize
    )
    runBlocking {
        client.sendFile(file).collect {
            State.updateClient(it)
        }
    }
}

fun main(args: Array<String>) {
    val sendFile = File(args[0])
    val writeFile = File(args.getOrNull(1) ?: "received-${sendFile.name}")
    State.numberOfFrames = ceil(sendFile.length().toDouble() / GBNSender.maxDataSize).toInt()

    val serverThread = thread {
        server(writeFile)
    }
    client(sendFile)
    serverThread.interrupt()
    serverThread.join()
}
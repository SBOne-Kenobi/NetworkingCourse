import server.StopWaitServer
import java.io.File

fun main() {
    val showProgress = !Settings.debugOutput
    val server = StopWaitServer(Settings.serverPort)

    val (receivedFile, socketAddress) = server.receiveFile(showProgress)
    val fileToSend = File(receivedFile.name + ".clone")
    receivedFile.copyTo(fileToSend, true)

    server.sendFile(socketAddress, fileToSend, showProgress)
}
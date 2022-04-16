import client.StopWaitClient
import java.io.File
import java.net.InetAddress

fun main(args: Array<String>) {
    val showProgress = !Settings.debugOutput
    val pathToFile = args.getOrNull(0) ?: error("Add path to sending file to arguments")
    val file = File(pathToFile)
    if (!file.exists()) {
        error("File $pathToFile does not exists")
    }
    if (!file.isFile) {
        error("Expected path to file, but got something else")
    }
    if (!file.canRead()) {
        error("Can't read file $pathToFile")
    }

    val hostAddress = InetAddress.getByName(Settings.serverHost)
    val client = StopWaitClient(hostAddress, Settings.serverPort)

    client.sendFile(file, showProgress)
    client.receiveFile(showProgress)
}
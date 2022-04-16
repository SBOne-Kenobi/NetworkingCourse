package util

import protocol.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.SocketAddress

fun ByteArray.toFrame(length: Int) =
    readStopWaitFrame(inputStream(0, length))

fun StopWaitFrame.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    writeStopWaitFrame(this, stream)
    return stream.toByteArray()
}

fun StopWaitSender.sendFile(socketAddress: SocketAddress, file: File, showProgress: Boolean = false) {
    val progressBar = ProgressBar()
    var bytesSent = 0
    val fileSize = file.length()

    if (showProgress) {
        println("Sending ${file.name} (${fileSize} bytes)")
        progressBar.start()
    }

    send(socketAddress, "$fileSize ${file.name}".toByteArray())
    file.forEachBlock(maxDataSize) { data, size ->
        send(socketAddress, data.sliceArray(0 until size))
        bytesSent += size
        progressBar.update(bytesSent.toFloat() / fileSize)
    }

    if (showProgress) {
        println("\nDone")
    }
}

fun StopWaitReceiver.receiveFile(showProgress: Boolean = false): Pair<File, SocketAddress> {
    var fileSize = 0L
    var fileName = ""
    var socketAddress: SocketAddress? = null
    receive { data, address ->
        socketAddress = address
        val message = data?.decodeToString() ?: error("Expected not empty data")
        fileSize = message.substringBefore(' ').toLong()
        fileName = message.substringAfter(' ')
    }

    val progressBar = ProgressBar()

    if (showProgress) {
        println("Start receive $fileName ($fileSize bytes)")
        progressBar.start()
    }

    val file = File(fileName)
    val outputStream = file.outputStream()
    var wroteBytes = 0L
    while (wroteBytes < fileSize) {
        receive { data, _ ->
            data?.let {
                outputStream.write(it)
                wroteBytes += it.size
                progressBar.update(wroteBytes.toFloat() / fileSize)
            }
        }
    }
    outputStream.flush()

    if (showProgress) {
        println("\nDone")
    }

    return file to socketAddress!!
}

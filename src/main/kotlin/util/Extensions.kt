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

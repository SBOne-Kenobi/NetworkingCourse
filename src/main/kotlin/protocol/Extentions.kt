package protocol

import java.io.ByteArrayOutputStream

fun ByteArray.toFrame(length: Int) =
    readStopWaitFrame(inputStream(0, length))

fun GBNFrame.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    writeGBNFrame(this, stream)
    return stream.toByteArray()
}
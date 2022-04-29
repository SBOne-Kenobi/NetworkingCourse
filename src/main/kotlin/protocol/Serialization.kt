package protocol

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val GBN_ACK_FLAG = 1 shl 0

const val GBN_HEADER_SIZE = 2

fun writeGBNFrame(gbnFrame: GBNFrame, outputStream: OutputStream) {
    try {
        outputStream.write(gbnFrame.frameNumber)
        var flags = 0
        if (gbnFrame.isAck)
            flags += GBN_ACK_FLAG
        outputStream.write(flags)
        gbnFrame.data?.let { outputStream.write(it) }
    } catch (e: IOException) {
        throw IOException("Failed to write GBNFrame", e)
    }
}

fun readStopWaitFrame(inputStream: InputStream): GBNFrame {
    try {
        val frameNumber = inputStream.read()
        val flags = inputStream.read()
        if (frameNumber == -1 || flags == -1) {
            error("Incomplete header")
        }
        val isAck = flags.and(GBN_ACK_FLAG) == GBN_ACK_FLAG
        val data = inputStream.readBytes()
        return GBNFrame(frameNumber, isAck, data)
    } catch (e: Exception) {
        throw IOException("Failed to read GBNFrame: ${e.message}", e)
    }
}
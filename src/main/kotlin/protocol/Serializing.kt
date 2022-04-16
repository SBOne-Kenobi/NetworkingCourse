package protocol

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val SW_ACK_FLAG = 1 shl 0

const val SW_HEADER_SIZE = 4

fun writeStopWaitFrame(stopWaitFrame: StopWaitFrame, outputStream: OutputStream) {
    val byteStream = ByteArrayOutputStream()
    try {
        byteStream.write(stopWaitFrame.frameNumber)
        var flags = 0
        if (stopWaitFrame.isAck)
            flags += SW_ACK_FLAG
        byteStream.write(flags)
        stopWaitFrame.data?.let {
            byteStream.write(it)
        }
        val bytes = byteStream.toByteArray()
        val checksum = computeChecksum(bytes)

        outputStream.write(stopWaitFrame.frameNumber)
        outputStream.write(flags)
        outputStream.write(checksum.toInt() shr 8)
        outputStream.write(checksum.and(0xFFu).toInt())
        stopWaitFrame.data?.let { outputStream.write(it) }
    } catch (e: IOException) {
        throw IOException("Failed to write StopWaitFrame", e)
    }
}

fun readStopWaitFrame(inputStream: InputStream): StopWaitFrame {
    try {
        val byteStream = ByteArrayOutputStream()

        val frameNumber = inputStream.read()
        byteStream.write(frameNumber)
        val flags = inputStream.read()
        byteStream.write(flags)
        val checksumHigh = inputStream.read()
        val checksumLow = inputStream.read()
        if (frameNumber == -1 || flags == -1 || checksumHigh == -1 || checksumLow == -1) {
            error("Incomplete header")
        }

        val checksum = ((checksumHigh.toUInt() shl 8) + checksumLow.toUInt()).toUShort()
        val isAck = flags.and(SW_ACK_FLAG) == SW_ACK_FLAG
        val data = inputStream.readBytes()
        byteStream.write(data)

        if (!validateChecksum(byteStream.toByteArray(), checksum)) {
            error("Invalid checksum")
        }

        return StopWaitFrame(frameNumber, isAck, data)
    } catch (e: Exception) {
        throw IOException("Failed to read StopWaitFrame: ${e.message}", e)
    }
}

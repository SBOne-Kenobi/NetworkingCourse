package protocol

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val SW_ACK_FLAG = 1 shl 0

const val SW_HEADER_SIZE = 2

fun writeStopWaitFrame(stopWaitFrame: StopWaitFrame, outputStream: OutputStream) {
    try {
        outputStream.write(stopWaitFrame.frameNumber)
        var flags = 0
        if (stopWaitFrame.isAck)
            flags += SW_ACK_FLAG
        outputStream.write(flags)

        stopWaitFrame.data?.let {
            outputStream.write(it)
        }
    } catch (e: IOException) {
        throw IOException("Failed to write StopWaitFrame", e)
    }
}

fun readStopWaitFrame(inputStream: InputStream): StopWaitFrame {
    try {
        val frameNumber = inputStream.read()
        val flags = inputStream.read()
        if (frameNumber == -1 || flags == -1) {
            error("Incomplete header")
        }
        val isAck = flags.and(SW_ACK_FLAG) == SW_ACK_FLAG
        val data = inputStream.readBytes()
        return StopWaitFrame(frameNumber, isAck, data)
    } catch (e: Exception) {
        throw IOException("Failed to read StopWaitFrame", e)
    }
}

package protocol

import Settings
import debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import util.toByteArray
import util.toFrame
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.net.SocketTimeoutException

class StopWaitSender(
    private val socket: DatagramSocket,
    private val receiveDatagramPacket: DatagramPacket,
    private val ackWaitTimeoutMillis: Int
) {
    private var currentFrameNumber = 0

    val maxDataSize
        get() = Settings.maxUPDSendSize - SW_HEADER_SIZE

    private suspend fun waitAck(address: SocketAddress): Boolean =
        withTimeoutOrNull(ackWaitTimeoutMillis.toLong()) {
            withContext(Dispatchers.IO) {
                do {
                    debug("Wait ack...")
                    try {
                        socket.receive(receiveDatagramPacket)
                    } catch (_: SocketTimeoutException) {
                        return@withContext false
                    }
                    var ack: StopWaitFrame? = null
                    try {
                        ack = receiveDatagramPacket.data.toFrame(receiveDatagramPacket.length)
                    } catch (e: IOException) {
                        System.err.println(e)
                    }
                    if (ack != null && !ack.isAck && receiveDatagramPacket.socketAddress == address) {
                        return@withContext true
                    }
                } while (
                    ack == null ||
                    !ack.isAck ||
                    ack.frameNumber != currentFrameNumber ||
                    receiveDatagramPacket.socketAddress != address
                )
                debug("Ack received")
                return@withContext true
            }
        } ?: false

    fun send(socketAddress: SocketAddress, data: ByteArray) {
        try {
            socket.soTimeout = ackWaitTimeoutMillis
            runBlocking {
                withContext(Dispatchers.IO) {
                    while (true) {
                        debug("Trying send data....")
                        val frame = StopWaitFrame(currentFrameNumber, false, data)
                        val bytes = frame.toByteArray()
                        val datagramPacket = DatagramPacket(bytes, bytes.size, socketAddress)
                        socket.send(datagramPacket)
                        if (waitAck(socketAddress)) break
                        debug("Failed\n")
                    }
                    currentFrameNumber = currentFrameNumber.xor(1)
                    debug("Done")
                    debug("---------------")
                }
            }
        } finally {
            socket.soTimeout = 0
        }
    }
}
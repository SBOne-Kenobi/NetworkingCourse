package protocol

import debug
import util.toByteArray
import util.toFrame
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress

class StopWaitReceiver(
    private val socket: DatagramSocket,
    private val receiveDatagramPacket: DatagramPacket
) {
    private var currentFrameNumber: Int? = null

    fun sendAck(ackPacket: StopWaitFrame, socketAddress: SocketAddress) {
        debug("Send ack")
        val bytes = ackPacket.toByteArray()
        val datagramPacket = DatagramPacket(bytes, bytes.size, socketAddress)
        socket.send(datagramPacket)
    }

    fun <T> receive(handler: (ByteArray?, SocketAddress) -> T): T {
        var data: ByteArray?
        while (true) {
            debug("Receive")
            socket.receive(receiveDatagramPacket)
            try {
                val frame = receiveDatagramPacket.data.toFrame(receiveDatagramPacket.length)
                if (frame.isAck) continue
                data = frame.data
                sendAck(frame.ackPacket, receiveDatagramPacket.socketAddress)
                if (currentFrameNumber != frame.frameNumber) {
                    currentFrameNumber = frame.frameNumber
                    break
                }
            } catch (e: IOException) {
                System.err.println(e.message)
                continue
            }
        }
        debug("Done")
        debug("----------")
        return handler(data, receiveDatagramPacket.socketAddress)
    }

}
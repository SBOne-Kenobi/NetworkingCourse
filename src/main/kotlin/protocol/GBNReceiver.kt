package protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress

class GBNReceiver(
    private val socket: DatagramSocket,
    private val receivePacket: DatagramPacket
) {

    private var nextFrameNumber = 0

    private fun sendAck(socketAddress: SocketAddress, ackFrame: GBNFrame) {
        val data = ackFrame.toByteArray()
        val packet = DatagramPacket(data, data.size, socketAddress)
        socket.send(packet)
    }

    fun run() = channelFlow {
        while (true) {
            socket.receive(receivePacket)
            try {
                val frame = receivePacket.data.toFrame(receivePacket.length)
                if (frame.frameNumber == nextFrameNumber) {
                    send(NewFrameEvent(frame))
                    nextFrameNumber += 1
                } else {
                    send(BadFrameEvent(frame))
                }
                sendAck(
                    receivePacket.socketAddress,
                    GBNFrame(nextFrameNumber, true)
                )
            } catch (e: IOException) {
                System.err.println(e.message)
                continue
            }
        }
    }.flowOn(Dispatchers.IO)

    sealed class Event
    data class NewFrameEvent(val frame: GBNFrame) : Event()
    data class BadFrameEvent(val frame: GBNFrame) : Event()
}
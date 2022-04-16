package server

import Settings
import protocol.StopWaitReceiver
import protocol.StopWaitSender
import util.LossDatagramSocket
import util.receiveFile
import util.sendFile
import java.io.File
import java.net.DatagramPacket
import java.net.SocketAddress

class StopWaitServer(port: Int) {
    private val socket = LossDatagramSocket(port, Settings.lossProbability)
    private val receiveBuffer = ByteArray(socket.receiveBufferSize)
    private val receiveDatagramPacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

    private val receiver = StopWaitReceiver(socket, receiveDatagramPacket)
    private val sender = StopWaitSender(socket, receiveDatagramPacket, Settings.ackWaitTimeoutMillis)

    fun <T> receiveBytes(handler: (ByteArray?, SocketAddress) -> T) =
        receiver.receive(handler)

    fun receiveFile(showProgress: Boolean = false) =
        receiver.receiveFile(showProgress)

    fun sendBytes(socketAddress: SocketAddress, byteArray: ByteArray) {
        sender.send(socketAddress, byteArray)
    }

    fun sendFile(socketAddress: SocketAddress, file: File, showProgress: Boolean = false) {
        sender.sendFile(socketAddress, file, showProgress)
    }

}
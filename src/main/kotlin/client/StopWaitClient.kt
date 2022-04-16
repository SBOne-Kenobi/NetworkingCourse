package client

import Settings
import protocol.StopWaitReceiver
import protocol.StopWaitSender
import util.LossDatagramSocket
import util.receiveFile
import util.sendFile
import java.io.File
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress

class StopWaitClient(
    serverAddress: InetAddress,
    serverPort: Int
) {
    private val serverSocketAddress = InetSocketAddress(serverAddress, serverPort)
    private val socket = LossDatagramSocket(Settings.lossProbability)
    private val buffer = ByteArray(socket.receiveBufferSize)
    private val receiveDatagramPacket = DatagramPacket(buffer, buffer.size)

    private val receiver = StopWaitReceiver(socket, receiveDatagramPacket)
    private val sender = StopWaitSender(socket, receiveDatagramPacket, Settings.ackWaitTimeoutMillis)

    fun sendBytes(byteArray: ByteArray) {
        sender.send(serverSocketAddress, byteArray)
    }

    fun sendFile(file: File, showProgress: Boolean = false) {
        sender.sendFile(serverSocketAddress, file, showProgress)
    }

    fun receiveBytes(): ByteArray? =
        receiver.receive { data, _ -> data }

    fun receiveFile(showProgress: Boolean = false) =
        receiver.receiveFile(showProgress).first

}
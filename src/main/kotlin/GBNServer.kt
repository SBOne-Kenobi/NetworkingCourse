import protocol.GBNReceiver
import java.net.DatagramPacket

class GBNServer(
    port: Int
) {
    private val socket = LossDatagramSocket(port, Settings.lossProbability)
    private val receiveBuffer = ByteArray(socket.receiveBufferSize)
    private val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

    private val receiver = GBNReceiver(socket, receivePacket)

    fun receiveData() =
        receiver.run()
}
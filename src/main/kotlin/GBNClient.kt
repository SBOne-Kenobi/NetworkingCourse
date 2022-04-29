import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import protocol.GBNSender
import java.io.File
import java.net.DatagramPacket
import java.net.InetSocketAddress

class GBNClient(
    serverHost: String,
    serverPort: Int,
    windowSize: Int
) {
    private val serverSocketAddress = InetSocketAddress(serverHost, serverPort)
    private val socket = LossDatagramSocket(Settings.lossProbability)
    private val receiveBuffer = ByteArray(socket.receiveBufferSize)
    private val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

    private val sender = GBNSender(socket, receivePacket, windowSize, Settings.timeout)

    fun sendFile(file: File): Flow<GBNSender.Event> {
        val dataFlow = channelFlow {
            file.forEachBlock(GBNSender.maxDataSize) { buffer, bytesRead ->
                runBlocking {
                    send(buffer.sliceArray(0 until bytesRead))
                }
            }
        }
        return sender.run(serverSocketAddress, dataFlow)
    }

}
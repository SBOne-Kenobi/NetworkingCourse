package speedtest.networking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import speedtest.Settings
import java.net.*

sealed interface Receiver {
    fun receive(address: InetAddress, port: Int): Flow<Pair<ByteArray, Long>>
}

object TCPReceiver : Receiver {
    override fun receive(address: InetAddress, port: Int): Flow<Pair<ByteArray, Long>> {
        val serverSocket = ServerSocket(port, 1, address).apply {
            soTimeout = Settings.timeoutToWaitConnection
        }
        try {
            val socket = serverSocket.accept()
            val input = socket.getInputStream()
            return channelFlow {
                try {
                    val buffer = ByteArray(Settings.packetSize)
                    while (input.readNBytes(buffer, 0, buffer.size) > 0) {
                        send(buffer.copyOf() to System.nanoTime())
                    }
                } finally {
                    socket.close()
                    serverSocket.close()
                }
            }.flowOn(Dispatchers.IO)
        } catch (e: Exception) {
            serverSocket.close()
            throw e
        }
    }
}

object UDPReceiver : Receiver {
    override fun receive(address: InetAddress, port: Int): Flow<Pair<ByteArray, Long>> {
        val socket = DatagramSocket(port, address).apply {
            soTimeout = Settings.timeoutToWaitConnection
        }
        val buffer = ByteArray(Settings.packetSize)
        val receivePacket = DatagramPacket(buffer, buffer.size)
        return channelFlow {
            socket.use { socket ->
                while (true) {
                    try {
                        socket.receive(receivePacket)
                        send(buffer.copyOf() to System.nanoTime())
                    } catch (_: SocketTimeoutException) {
                        break
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
    }
}

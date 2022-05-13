package speedtest.networking

import speedtest.Settings
import java.io.OutputStream
import java.net.*

sealed interface Sender : AutoCloseable {
    fun connect(address: InetAddress, port: Int)
    fun send(data: ByteArray)
    fun disconnect()
}

class TCPSender : Sender {
    private var socket: Socket? = null
    private var output: OutputStream? = null

    override fun connect(address: InetAddress, port: Int) {
        socket = Socket().apply {
            connect(InetSocketAddress(address, port), Settings.timeoutToWaitConnection)
        }
        output = socket!!.getOutputStream()
    }

    override fun send(data: ByteArray) {
        output!!.write(data)
        output!!.flush()
    }

    override fun disconnect() {
        socket?.close()
        socket = null
        output = null
    }

    override fun close() {
        disconnect()
    }
}

class UDPSender : Sender {
    private val socket = DatagramSocket()

    override fun connect(address: InetAddress, port: Int) {
        socket.connect(InetSocketAddress(address, port))
    }

    override fun send(data: ByteArray) {
        socket.send(DatagramPacket(data, data.size))
    }

    override fun disconnect() {}

    override fun close() {
        socket.close()
    }
}

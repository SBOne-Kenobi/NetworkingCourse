import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

private fun createSocket(address: InetAddress, port: Int) =
    DatagramSocket(port, address).apply {
        broadcast = true
        receiveBufferSize = 8
    }

private fun DatagramSocket.receiveLong(packet: DatagramPacket): Long {
    receive(packet)
    return ByteBuffer.wrap(packet.data).long
}

fun main() {
    val address = InetAddress.getByName("0.0.0.0")
    val port = 8080

    val socket = createSocket(address, port)
    val bytes = ByteArray(8)
    val packet = DatagramPacket(bytes, 8)
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss")

    while (true) {
        val time = socket.receiveLong(packet)
        println(sdf.format(Date(time)))
    }
}
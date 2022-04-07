import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun main(args: Array<String>) {
    val pingTries = 10
    val sleepTime: Long = 1000

    val host = InetAddress.getByName(args.getOrNull(0) ?: "127.0.0.1")
    val port = args.getOrNull(1)?.toIntOrNull() ?: 8080

    val socket = DatagramSocket().apply {
        connect(host, port)
    }

    for (i in 1..pingTries) {
        val start = System.currentTimeMillis()
        val message = "Ping $i $start".toByteArray()
        println("Send ${message.decodeToString()}")
        socket.send(DatagramPacket(message, message.size))
        Thread.sleep(sleepTime)
    }

    socket.close()

}
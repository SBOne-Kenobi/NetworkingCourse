import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.random.Random

private fun isLoss(lossProb: Double, random: Random): Boolean {
    return random.nextDouble() < lossProb
}

fun main(args: Array<String>) {
    val port = args.getOrNull(0)?.toIntOrNull() ?: 8080
    val lossProb = 0.2
    val random = Random(228)

    val socket = DatagramSocket(port)
    val bytes = ByteArray(socket.receiveBufferSize)
    val packet = DatagramPacket(bytes, bytes.size)

    while (true) {
        socket.receive(packet)
        val addr = "${packet.address.hostAddress}:${packet.port}"
        val receive = bytes.decodeToString(endIndex = packet.length)
        println("Received \"$receive\" from $addr")
        if (isLoss(lossProb, random)) {
            println("Loss: $addr")
            continue
        }
        val response = receive.uppercase().toByteArray()
        println("Send \"${response.decodeToString()}\" to $addr")
        socket.send(DatagramPacket(response, response.size, packet.socketAddress))
    }
}
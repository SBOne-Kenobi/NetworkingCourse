import java.net.*
import java.nio.ByteBuffer


private fun createSocket() =
    DatagramSocket().apply {
        broadcast = true
        sendBufferSize = 8
    }

private fun buildPacket(value: Long, address: InetAddress, port: Int): DatagramPacket {
    val bytes = ByteArray(8)
    ByteBuffer.wrap(bytes).putLong(value)
    return DatagramPacket(bytes, 8, address, port)
}

fun listAllBroadcastAddresses(): List<InetAddress> {
    val broadcastList = mutableListOf(
        InetAddress.getByName("255.255.255.255")
    )
    println("Local: ${broadcastList[0].hostAddress}")
    val interfaces = NetworkInterface.getNetworkInterfaces()
    while (interfaces.hasMoreElements()) {
        val networkInterface = interfaces.nextElement()
        if (networkInterface.isLoopback || !networkInterface.isUp) {
            continue
        }
        networkInterface.interfaceAddresses.stream()
            .map { it.broadcast }
            .filter { it != null }
            .forEach {
                println("${networkInterface.displayName}: ${it.hostAddress}")
                broadcastList.add(it)
            }
    }
    return broadcastList
}

private fun DatagramSocket.broadcastAll(value: Long, port: Int, addressList: List<InetAddress>) {
    addressList.forEach {
        send(buildPacket(value, it, port))
    }
}

fun main() {
    val addressList = listAllBroadcastAddresses()
    val port = 8080
    val delayMillis: Long = 1000

    val broadcastAddress = addressList[0]

    val socket = createSocket()
    println("Start broadcasting ${broadcastAddress.hostAddress}")
    while (true) {
        val currentTime = System.currentTimeMillis()
//        socket.broadcastAll(currentTime, port, addressList)
        socket.send(buildPacket(currentTime, broadcastAddress, port))
        Thread.sleep(delayMillis)
    }
}
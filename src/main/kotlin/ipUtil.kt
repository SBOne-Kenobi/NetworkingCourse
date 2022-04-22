import java.net.InterfaceAddress
import java.net.NetworkInterface
import kotlin.streams.toList

private fun getInterfaceAddress(): InterfaceAddress {
    return NetworkInterface
        .networkInterfaces()
        .toList()
        .filter {
            it.isUp && !it.isLoopback
        }
        .flatMap {
            it.interfaceAddresses
        }
        .first {
            it.address.hostAddress.startsWith("192.168")
        }
}

private val InterfaceAddress.ip: String
    get() = address.hostAddress

private val InterfaceAddress.subnetwork: String
    get() = (0..3).map { i ->
        (8 - (networkPrefixLength - i * 8).coerceIn(0, 8)).let {
            UByte.MAX_VALUE.toInt().shr(it).shl(it)
        }
    }.joinToString(".")

fun main() {
    val interfaceAddress = getInterfaceAddress()
    val ipAddress = interfaceAddress.ip
    val subnetwork = interfaceAddress.subnetwork
    println("Ip address: $ipAddress")
    println("Subnetwork mask: $subnetwork")
}

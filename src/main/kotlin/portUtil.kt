import java.net.InetAddress
import java.net.Socket

fun getAvailablePorts(
    address: InetAddress,
    from: Int,
    to: Int
): List<Int> {
    return (from..to).filter { port ->
        // try to connect, if connection success it means there's server on this port
        runCatching {
            Socket(address, port).close()
        }.isFailure
    }
}

fun main() {
    print("Enter ip address: ")
    val address = InetAddress.getByName(readLine())
    print("Enter minimal port: ")
    val min = readLine()!!.toInt()
    print("Enter maximal port: ")
    val max = readLine()!!.toInt()
    println("Available ports for ${address.hostAddress}: ")

    val ports = getAvailablePorts(address, min, max)
    println(ports.chunked(15) {
        ports.joinToString(", ")
    }.joinToString(",\n"))
}
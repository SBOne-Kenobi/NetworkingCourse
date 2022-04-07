import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.max
import kotlin.math.min

private fun Double.format(digits: Int) =
    "%.${digits}f".format(this).replace(',', '.')

class Statistics {
    private var minRTT: Long? = null
    private var maxRTT: Long? = null
    private var cnt = 0
    private var successCnt = 0
    private var avgRTT = 0.0

    fun update(rtt: Long?) {
        cnt += 1
        if (rtt != null) {
            successCnt += 1
            minRTT = minRTT?.let { min(it, rtt) } ?: rtt
            maxRTT = maxRTT?.let { max(it, rtt) } ?: rtt
            avgRTT += (rtt - avgRTT) / successCnt
        }
    }

    override fun toString(): String {
        return """Statistics:
            |   Sent: $cnt, Received: $successCnt, Lost: ${cnt - successCnt} (${((cnt - successCnt).toDouble() / cnt).format(2)}% loss)
            |   RTT min = ${minRTT}ms, RTT max = ${maxRTT}ms, RTT avg = ${avgRTT.format(2)}ms
        """.trimMargin()
    }
}

fun main(args: Array<String>) {
    val pingTries = 10
    val timeoutMillis = 1000

    val host = InetAddress.getByName(args.getOrNull(0) ?: "127.0.0.1")
    val port = args.getOrNull(1)?.toIntOrNull() ?: 8080

    val socket = DatagramSocket().apply {
        connect(host, port)
        soTimeout = timeoutMillis
    }

    val bytes = ByteArray(socket.receiveBufferSize)
    val packet = DatagramPacket(bytes, bytes.size)
    val sf = SimpleDateFormat("dd.MM.yyyy hh:mm:ss")
    val statistics = Statistics()

    for (i in 1..pingTries) {
        val start = System.currentTimeMillis()
        val message = "Ping $i ${sf.format(Date(start))}".toByteArray()
        socket.send(DatagramPacket(message, message.size))
        var rtt: Long? = null
        try {
            socket.receive(packet)
            rtt = System.currentTimeMillis() - start
            println("Received: ${bytes.decodeToString(endIndex = packet.length)}")
            println("RTT: ${rtt}ms")
        } catch (e: SocketTimeoutException) {
            println("Request timed out")
        }
        statistics.update(rtt)
        println("$statistics\n")
    }

    socket.close()

}
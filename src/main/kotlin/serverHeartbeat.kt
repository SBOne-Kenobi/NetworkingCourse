import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.schedule
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.random.Random


private fun Double.format(digits: Int) =
    "%.${digits}f".format(this).replace(',', '.')

class ClientInfo {
    var lastSystemTimeUpdate = System.currentTimeMillis()
        private set
    var lastNumSeq: Long = 0
        private set
    var lastTime: Long? = null
        private set
    var lostNumber: Long = 0
        private set

    fun update(seqNumber: Long, time: Long): Long? {
        if (seqNumber <= lastNumSeq)
            return null
        lostNumber += (seqNumber - lastNumSeq - 1)
        return (lastTime?.let { time - it } ?: 0).also {
            lastNumSeq = seqNumber
            lastTime = time
            lastSystemTimeUpdate = System.currentTimeMillis()
        }
    }
}

class HeartbeatServer(port: Int, private val timeoutMillis: Long, private val debugOutput: Boolean = false) : AutoCloseable {
    private val lossProb = 0.2
    private val random = Random(228)

    private val socket = DatagramSocket(port)
    private val receiveBytes = ByteArray(socket.receiveBufferSize)
    private val receivePacket = DatagramPacket(receiveBytes, receiveBytes.size)

    private val clientsInfo = mutableMapOf<SocketAddress, ClientInfo>()

    private val timer = Timer()
    private val lock = ReentrantLock()

    private fun isLoss(): Boolean {
        return random.nextDouble() < lossProb
    }

    private fun checkClients() {
        lock.withLock {
            val clientsToRemove = mutableListOf<SocketAddress>()
            val currentSystemTime = System.currentTimeMillis()
            clientsInfo.forEach { (addr, clientInfo) ->
                if (currentSystemTime - clientInfo.lastSystemTimeUpdate > timeoutMillis) {
                    clientsToRemove.add(addr)
                }
            }
            clientsToRemove.forEach {
                println("Client $it stopped.")
                clientsInfo.remove(it)
            }
        }
    }

    private fun update(address: SocketAddress, seqNumber: Long, time: Long): Pair<ClientInfo, Long>? {
        lock.withLock {
            val clientInfo = clientsInfo.getOrPut(address) { ClientInfo() }
            return clientInfo.update(seqNumber, time)?.let { clientInfo to it }
        }
    }

    fun debug(message: String) {
        if (debugOutput) println(message)
    }

    fun run() {
        val period = max(1, timeoutMillis / 2)
        timer.schedule(period, period) {
            checkClients()
        }
        while (true) {
            socket.receive(receivePacket)
            if (isLoss()) {
                debug("Loss: ${receivePacket.socketAddress}")
                continue
            }
            val receive = receiveBytes.decodeToString(endIndex = receivePacket.length)
            debug("Received \"$receive\" from ${receivePacket.socketAddress}")
            val (seqNumber, time) = receive.split(' ').let {
                it[1].toLong() to it[2].toLong()
            }
            update(receivePacket.socketAddress, seqNumber, time)?.let { (clientInfo, timeDiff) ->
                val loss = (clientInfo.lostNumber.toDouble() / clientInfo.lastNumSeq)
                println("${receivePacket.socketAddress}: " +
                        "lost number = ${clientInfo.lostNumber} (${loss.format(2)}% loss), " +
                        "time diff = ${timeDiff}ms")
            } ?: debug("Ignored")
        }
    }

    override fun close() {
        socket.close()
    }
}

fun main(args: Array<String>) {
    val port = args.getOrNull(0)?.toIntOrNull() ?: 8080
    val timeoutMillis = args.getOrNull(1)?.toLongOrNull() ?: 10000
    HeartbeatServer(port, timeoutMillis, false).run()
}
package rip

import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

typealias DataType = ByteArray

class ThreadCommunication(
    private val host: Node
) : Communication {

    companion object {

        private val targetChannels = ConcurrentHashMap<Node, MyChannel<Pair<Node, DataType>>>()

        private fun get(target: Node) =
            targetChannels.getOrPut(target) {
                MyChannel()
            }
    }

    override suspend fun send(target: Node, connection: Connection) {
        if (Settings.allowDelayCommunication)
            delay(Settings.delayCommunication)
        val data = RIP.buildPackage(RIPData(
            RIP.Command.RESPONSE, connection.to.ip,
            connection.nextHop.ip, connection.cost
        ))
        get(target).send(host to data)
    }

    override suspend fun receive(): Pair<Node, Connection> {
        val (from, data) = get(host).receive()
        val (command, destination, nextHop, cost) = RIP.parsePackage(data)
        return from to Connection(destination.node, cost, nextHop.node)
    }

    override fun close() {}
}
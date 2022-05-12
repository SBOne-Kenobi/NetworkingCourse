package rip

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

typealias Cost = Int
const val ZERO: Cost = 0

data class Connection(val to: Node, val cost: Cost, val nextHop: Node = to)

class Node(
    val ip: InetAddress,
    private val communicationFactory: Node.() -> Communication
) {
    companion object {

        private val nodeByAddress = mutableMapOf<InetAddress, Node>()

        private fun register(node: Node) {
            nodeByAddress[node.ip] = node
        }

        fun getByAddress(address: InetAddress): Node {
            return nodeByAddress[address]!!
        }
    }

    init {
        register(this)
    }

    private val neighbours = ConcurrentHashMap<Node, Cost>()
    private val available = ConcurrentHashMap<Node, Connection>()

    val computed: Map<Node, Connection>
        get() = available.toMap()

    private val channelSend = Channel<Connection>(Channel.UNLIMITED)

    private lateinit var communication: Communication

    private lateinit var senderThread: Thread
    private lateinit var receiverThread: Thread
    private lateinit var broadcasterThread: Thread

    private suspend fun send(connection: Connection) {
        neighbours.forEach { (target, _) ->
            communication.send(target, connection)
        }
    }

    private fun receive(): Pair<Node, Connection> = runBlocking {
        communication.receive()
    }

    private fun update(node: Node, connection: Connection) {
        if (connection.to == this) return
        val edgeCost = neighbours[node] ?: return
        val newCost = connection.cost + edgeCost
        var changed = false
        val newConnection = available.compute(connection.to) { to, oldConnection ->
            if (oldConnection == null || oldConnection.cost > newCost) {
                changed = true
                Connection(to, newCost, node)
            } else {
                oldConnection
            }
        }
        if (changed) channelSend.trySend(newConnection!!)
    }

    private fun initBroadcaster() {
        broadcasterThread = thread {
            try {
                runBlocking {
                    while (!Thread.interrupted()) {
                        available.forEach { (_, connection) ->
                            send(connection)
                        }
                        delay(Settings.updateTimer)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun initSender() {
        senderThread = thread {
            try {
                runBlocking {
                    channelSend.receiveAsFlow().cancellable().collect {
                        send(it)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun initReceiver() {
        receiverThread = thread {
            try {
                while (!Thread.interrupted()) {
                    val (node, connection) = receive()
                    update(node, connection)
                }
            } catch (_: Exception) {}
        }
    }

    fun addNeighbour(connection: Connection) {
        neighbours[connection.to] = connection.cost
        available[connection.to] = connection
    }

    fun updateNeighbour(connection: Connection) {
        neighbours.compute(connection.to) { _, oldCost ->
            require(oldCost == null || oldCost >= connection.cost)
            connection.cost
        }
        update(connection.to, Connection(connection.to, ZERO))
    }

    fun start() {
        initBroadcaster()
        initSender()
        initReceiver()
        communication = communicationFactory()
    }

    fun stop() {
        broadcasterThread.stopThread()
        senderThread.stopThread()
        receiverThread.stopThread()
        communication.close()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node

        if (ip != other.ip) return false

        return true
    }

    override fun hashCode(): Int {
        return ip.hashCode()
    }

    override fun toString(): String {
        return ip.hostAddress
    }


}
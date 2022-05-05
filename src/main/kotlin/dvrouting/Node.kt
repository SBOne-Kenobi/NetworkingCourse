package dvrouting

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

typealias Cost = Double
const val ZERO: Cost = 0.0

data class Connection(val to: Node, val cost: Cost)

class Node(
    private val communicationFactory: Node.() -> Communication
) {
    private val neighbours = ConcurrentHashMap<Node, Cost>()
    private val available = ConcurrentHashMap<Node, Cost>()

    val computed: Map<Node, Cost>
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
        available.compute(connection.to) { _, oldCost ->
            if (oldCost == null || oldCost > newCost) {
                changed = true
                newCost
            } else {
                oldCost
            }
        }
        if (changed) channelSend.trySend(Connection(connection.to, newCost))
    }

    private fun initBroadcaster() {
        broadcasterThread = thread {
            try {
                runBlocking {
                    while (!Thread.interrupted()) {
                        available.forEach { (to, cost) ->
                            send(Connection(to, cost))
                        }
                        delay(Settings.delayBroadcast)
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
        available[connection.to] = connection.cost
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

}
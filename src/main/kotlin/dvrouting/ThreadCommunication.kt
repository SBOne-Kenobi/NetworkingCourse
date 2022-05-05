package dvrouting

import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

class ThreadCommunication(
    private val host: Node
) : Communication {

    companion object {
        private val targetChannels = ConcurrentHashMap<Node, MyChannel<Pair<Node, Connection>>>()

        private fun get(target: Node) =
            targetChannels.getOrPut(target) {
                MyChannel()
            }
    }

    override suspend fun send(target: Node, connection: Connection) {
        if (Settings.allowDelayCommunication)
            delay(Settings.delayCommunication)
        get(target).send(host to connection)
    }

    override suspend fun receive(): Pair<Node, Connection> {
        return get(host).receive()
    }

    override fun close() {}
}
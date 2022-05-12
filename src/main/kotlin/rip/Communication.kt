package rip

interface Communication : AutoCloseable {
    suspend fun send(target: Node, connection: Connection)
    suspend fun receive(): Pair<Node, Connection>
}
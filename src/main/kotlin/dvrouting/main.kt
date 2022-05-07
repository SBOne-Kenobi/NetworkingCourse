package dvrouting

fun initNodes(): List<Node> {
    val nodes = List(4) {
        Node { ThreadCommunication(this) }
    }

    nodes[0].addNeighbour(Connection(nodes[1], 1.0))
    nodes[0].addNeighbour(Connection(nodes[2], 3.0))
    nodes[0].addNeighbour(Connection(nodes[3], 7.0))

    nodes[1].addNeighbour(Connection(nodes[0], 1.0))
    nodes[1].addNeighbour(Connection(nodes[2], 1.0))

    nodes[2].addNeighbour(Connection(nodes[0], 3.0))
    nodes[2].addNeighbour(Connection(nodes[1], 1.0))
    nodes[2].addNeighbour(Connection(nodes[3], 2.0))

    nodes[3].addNeighbour(Connection(nodes[0], 7.0))
    nodes[3].addNeighbour(Connection(nodes[2], 2.0))

    return nodes
}

fun print(nodes: List<Node>) {
    val separator = "--------------\n"
    println(nodes.mapIndexed { index, node ->
        buildString {
            appendLine("Node $index:")
            node.computed
                .toSortedMap(compareBy { nodes.indexOf(it) })
                .forEach { (to, cost) ->
                    appendLine("Node ${nodes.indexOf(to)} -> $cost")
                }
        }
    }.joinToString(separator, separator, separator))
}

fun List<Node>.updateEdge(n1: Int, n2: Int, cost: Cost) {
    get(n1).updateNeighbour(Connection(get(n2), cost))
    get(n2).updateNeighbour(Connection(get(n1), cost))
    println("Edge $n1-$n2: changed on $cost")
}

fun main() {
    val nodes = initNodes()

    nodes.forEach { it.start() }

    Thread.sleep(Settings.timeoutToFit)
// uncomment and set Settings.allowDelayCommunication true to receive incomplete state
//    Thread.sleep(Settings.timeoutToFit / 5)

    print(nodes)

    nodes.updateEdge(1, 2, 0.0)
// other way for checking
//    nodes.updateEdge(0, 3, 1.0)
//    nodes.updateEdge(0, 2, 1.0)

    Thread.sleep(Settings.timeoutToFit)
    nodes.forEach { it.stop() }
    print(nodes)
}
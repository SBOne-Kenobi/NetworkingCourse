package rip

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.InetAddress
import java.net.URL
import java.nio.file.Path

fun initNodes(path: URL): List<Node> {
    val nodes = mutableListOf<Node>()
    val config = Json.parseToJsonElement(path.readText())
    config as JsonObject

    config.entries.map { (addressString, neighbours) ->
        val node = Node(InetAddress.getByName(addressString)) { ThreadCommunication(this) }
        nodes.add(node)
        node to (neighbours as JsonArray).map { address ->
            InetAddress.getByName((address as JsonPrimitive).content)
        }
    }.forEach { (node, neighbours) ->
        neighbours.forEach { address ->
            node.addNeighbour(Connection(address.node, 1))
        }
    }

    return nodes
}

fun String.center(width: Int) =
    "%${length + (width - length) / 2}s".format(this)

fun getNodeState(titleFormat: String, node: Node, state: Map<Node, Connection>): String {
    val width = 16
    val metricWidth = 13
    val format = "| %-${width}s | %-${width}s | %-${width}s | %-${metricWidth}s |"
    return buildString {
        appendLine(titleFormat.format(node.ip.hostAddress))
        appendLine(format.format(
            "[Source IP]".center(width),
            "[Destination IP]".center(width),
            "[Next Hop]".center(width),
            "[Metric]".center(metricWidth)
        ))
        state.forEach { (_, connection) ->
            appendLine(format.format(
                node.ip.hostAddress.center(width),
                connection.to.ip.hostAddress.center(width),
                connection.nextHop.ip.hostAddress.center(width),
                connection.cost.toString().center(metricWidth)
            ))
        }
    }
}

fun getNodeStateHistory(node: Node): String {
    val separator = "${"-".repeat(74)}\n"
    var i = 0
    return StateHistory.getHistory(node).joinToString(separator) { state ->
        i += 1
        getNodeState("Simulation step $i of router %s", node, state)
    }
}

fun printState(nodes: List<Node>) {
    val len = if (Settings.collectHistory) 85 else 74
    val separator = "${"_".repeat(len)}\n"
    println(nodes.joinToString(separator, separator, separator) { node ->
        if (Settings.collectHistory)
            getNodeStateHistory(node)
        else
            getNodeState("Final state of router %s table:", node, node.computed)
    })
}

fun main(args: Array<String>) {
    val configURL = args.getOrNull(0)?.let { Path.of(it).toUri().toURL() }
        ?: ClassLoader.getSystemResource("config.json")
    val nodes = initNodes(configURL)

    StateHistory.start()
    nodes.forEach {
        if (Settings.collectHistory) {
            StateHistory.addNodeState(it, it.computed)
        }
        it.start()
    }
    Thread.sleep(Settings.timeoutToFit)
    nodes.forEach { it.stop() }
    StateHistory.stop()
    printState(nodes)
}
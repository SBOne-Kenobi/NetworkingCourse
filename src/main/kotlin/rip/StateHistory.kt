package rip

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

private typealias State = Map<Node, Connection>

object StateHistory {
    private val history = mutableMapOf<Node, MutableList<State>>()
    private val actionChannel = Channel<Pair<Node, State>>(Channel.UNLIMITED)
    private lateinit var updaterThread: Thread

    fun start() {
        updaterThread = thread {
            try {
                stateUpdater()
            } catch (_: InterruptedException) {}
        }
    }

    fun stop() {
        updaterThread.stopThread()
    }

    private fun stateUpdater() {
        runBlocking {
            actionChannel.receiveAsFlow().cancellable().collect { (node, state) ->
                history.getOrPut(node) {
                    mutableListOf()
                }.add(state)
            }
        }
    }

    fun addNodeState(node: Node, state: State) {
        actionChannel.trySend(node to state)
    }

    fun getHistory(node: Node): List<State> =
        history[node]!!

}
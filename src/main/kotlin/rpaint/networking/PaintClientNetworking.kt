package rpaint.networking

import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.runBlocking
import rpaint.Settings
import rpaint.control.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.concurrent.thread

class PaintClientNetworking(
    private val controller: PaintClientController
) : PaintNetworking {

    init {
        controller.networking = this
    }

    private lateinit var eventConsumerThread: Thread

    private val socket = DatagramSocket()
    private val serverSocketAddress = InetSocketAddress(
        Settings.serverHost, Settings.serverPort
    )

    private fun send(data: ByteArray) {
        val packet = DatagramPacket(data, data.size, serverSocketAddress)
        socket.send(packet)
    }

    private fun Event.toByteArray() : ByteArray {
        return when(this) {
            Clear ->
                "Clear"
            is DrawLine ->
                "DrawLine|${oldPoint.x}|${oldPoint.y}|${newPoint.x}|${newPoint.y}"
            is SetColor ->
                "SetColor|${color.red}|${color.green}|${color.blue}|${color.alpha}"
        }.toByteArray()
    }

    private fun subscribeOnEvents() {
        eventConsumerThread = thread {
            runBlocking {
                controller.eventFlow.cancellable().collect { event ->
                    send(event.toByteArray())
                }
            }
        }
    }

    override fun start() {
        socket.connect(serverSocketAddress)
        subscribeOnEvents()
    }

    override fun stop() {
        eventConsumerThread.interrupt()
        eventConsumerThread.join(Settings.waitForThreadStopMillis)
        socket.close()
    }
}
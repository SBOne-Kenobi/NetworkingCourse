package rpaint.networking

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.runBlocking
import rpaint.Settings
import rpaint.control.*
import java.awt.Color
import java.awt.Point
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.concurrent.thread

class PaintServerNetworking(
    private val controller: PaintServerController
) : PaintNetworking {

    init {
        controller.networking = this
    }

    private val socket = DatagramSocket(null)
    private val receiveBuffer = ByteArray(socket.receiveBufferSize)
    private val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

    private val dataChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private lateinit var dataHandlerThread: Thread
    private lateinit var receiverThread: Thread

    private fun parseEvent(bytes: ByteArray): Event? {
        val info = bytes.decodeToString().split("|")
        return when (info[0]) {
            "Clear" ->
                Clear
            "DrawLine" ->
                DrawLine(
                    Point(info[1].toInt(), info[2].toInt()),
                    Point(info[3].toInt(), info[4].toInt())
                )
            "SetColor" ->
                SetColor(Color(
                    info[1].toInt(), info[2].toInt(),
                    info[3].toInt(), info[4].toInt()
                ))
            else -> null
        }
    }

    private fun receive() {
        socket.receive(receivePacket)
        dataChannel.trySend(receiveBuffer.sliceArray(0 until receivePacket.length))
    }

    private fun initDataHandler() {
        dataHandlerThread = thread {
            runBlocking {
                dataChannel.consumeAsFlow().cancellable().collect { data ->
                    when(val event = parseEvent(data)) {
                        Clear -> controller.clear()
                        is DrawLine -> controller.drawLine(event.oldPoint, event.newPoint)
                        is SetColor -> controller.setColor(event.color)
                        null -> {}
                    }
                }
            }
        }
    }

    private fun initReceiver() {
        receiverThread = thread {
            while (!Thread.interrupted()) {
                receive()
            }
        }
    }

    override fun start() {
        socket.bind(InetSocketAddress(Settings.serverHost, Settings.serverPort))
        initDataHandler()
        initReceiver()
    }

    override fun stop() {
        dataHandlerThread.interrupt()
        receiverThread.interrupt()
        receiverThread.join(Settings.waitForThreadStopMillis)
        dataHandlerThread.join(Settings.waitForThreadStopMillis)
        socket.close()
    }
}
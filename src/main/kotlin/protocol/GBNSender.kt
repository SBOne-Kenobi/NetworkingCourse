package protocol

import Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.util.concurrent.Executors
import kotlin.math.max

class GBNSender(
    private val socket: DatagramSocket,
    private val receivePacket: DatagramPacket,
    private val windowSize: Int,
    private val timeout: Long
) {
    companion object {
        const val maxDataSize = Settings.maxDatagramPacketSize - GBN_HEADER_SIZE
    }

    private var baseFrameNumber = 0
    private var nextFrameNumber = 0

    private var ackChannel: Channel<GBNFrame>? = null
    private var ackReceiverExecutor = Executors.newSingleThreadExecutor()

    private fun runAckReceiver() {
        ackChannel = Channel()
        ackReceiverExecutor.execute {
            try {
                runBlocking(Dispatchers.IO) {
                    while (!Thread.interrupted()) {
                        socket.receive(receivePacket)
                        try {
                            val frame = receivePacket.data.toFrame(receivePacket.length)
                            if (frame.isAck) {
                                ackChannel?.send(frame) ?: break
                            }
                        } catch (e: IOException) {
                            System.err.println(e.message)
                            continue
                        }
                    }
                }
            } catch (_: InterruptedException) {}
        }
    }

    private fun send(socketAddress: SocketAddress, frame: GBNFrame) {
        val data = frame.toByteArray()
        val packet = DatagramPacket(data, data.size, socketAddress)
        socket.send(packet)
    }

    fun run(socketAddress: SocketAddress, dataFlow: Flow<ByteArray>) = channelFlow {
        var startTime = System.currentTimeMillis()
        val sentFramesWithoutAck = mutableListOf<GBNFrame>()

        val checkAck: suspend ProducerScope<*>.() -> Unit = {
            while (true) {
                val ackFrame = ackChannel!!.tryReceive().getOrNull() ?: break
                baseFrameNumber = max(baseFrameNumber, ackFrame.frameNumber)
                while (
                    sentFramesWithoutAck.isNotEmpty() &&
                    sentFramesWithoutAck.first().frameNumber < baseFrameNumber
                ) sentFramesWithoutAck.removeFirst()
                send(AckReceivedEvent(ackFrame))
                startTime = System.currentTimeMillis()
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                startTime = System.currentTimeMillis()
                sentFramesWithoutAck.forEach {
                    send(socketAddress, it)
                }
                send(TimeoutEvent(baseFrameNumber, nextFrameNumber - 1))
            }
        }

        dataFlow.cancellable().collect { data ->
            var dataSent = false
            while (!dataSent) {
                if (nextFrameNumber < baseFrameNumber + windowSize) {
                    val frame = GBNFrame(nextFrameNumber, false, data)
                    send(socketAddress, frame)
                    send(FrameSentEvent(frame))
                    nextFrameNumber += 1
                    sentFramesWithoutAck.add(frame)
                    dataSent = true
                }
                checkAck()
            }
        }
        while (sentFramesWithoutAck.isNotEmpty()) {
            checkAck()
        }
    }.onStart {
        runAckReceiver()
    }.onCompletion {
        ackChannel?.close()
        ackChannel = null
        ackReceiverExecutor.shutdownNow()
    }

    sealed class Event
    data class FrameSentEvent(val frame: GBNFrame) : Event()
    data class AckReceivedEvent(val ackFrame: GBNFrame) : Event()
    data class TimeoutEvent(val first: Int, val last: Int) : Event()
}
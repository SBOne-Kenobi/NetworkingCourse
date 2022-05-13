package speedtest.networking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import speedtest.controls.Protocol
import speedtest.controls.ServerController
import speedtest.toLong
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import kotlin.math.max

class ServerNetworking(
    private val controller: ServerController
) : BaseNetworking {

    private lateinit var protocol: Protocol
    private val receivers = mapOf(
        Protocol.TCP to TCPReceiver,
        Protocol.UDP to UDPReceiver
    )

    init {
        controller.networking = this
    }

    fun setProtocol(protocol: Protocol) {
        this.protocol = protocol
    }

    fun receive(address: InetAddress, port: Int) : Flow<Triple<Int, Int, Float>> {
        val receiver = receivers[protocol]
        if (receiver == null) {
            controller.showNetworkingError("Unsupported protocol $protocol")
            return emptyFlow()
        }
        return try {
            var nextPacket = 0
            var received = 0
            var avgRate = 0f
            receiver.receive(address, port).map { (data, receivedTime) ->
                val input = ByteArrayInputStream(data)
                val seq = input.readNBytes(4).toLong().toInt()
                val timestamp = input.readNBytes(8).toLong()
                val rate = 1e6f * data.size.toFloat() / max(receivedTime - timestamp, 1)
                if (seq >= nextPacket) {
                    received += 1
                    nextPacket = seq + 1
                    avgRate += (rate - avgRate) / received
                }
                Triple(received, nextPacket, avgRate)
            }.flowOn(Dispatchers.IO)
        } catch (e: IOException) {
            controller.showNetworkingError(e.message!!)
            emptyFlow()
        }
    }

    override fun start() {}

    override fun stop() {}
}
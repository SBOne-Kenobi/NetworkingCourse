package speedtest.networking

import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.runBlocking
import speedtest.Settings
import speedtest.controls.ClientController
import speedtest.controls.Protocol
import speedtest.stopThread
import speedtest.toByteArray
import java.io.IOException
import java.net.InetAddress
import kotlin.concurrent.thread
import kotlin.random.Random

class ClientNetworking(
    private val controller: ClientController
) : BaseNetworking {

    private lateinit var protocol: Protocol
    private val sockets: Map<Protocol, Sender> =
        mapOf(
            Protocol.TCP to TCPSender(),
            Protocol.UDP to UDPSender(),
        )

    init {
        controller.networking = this
    }

    private lateinit var observerThread: Thread

    private fun send(address: InetAddress, port: Int, numberOfPackages: Int) {
        val sender = sockets[protocol]
        if (sender == null) {
            controller.showNetworkingError("Unsupported protocol $protocol")
            return
        }
        try {
            sender.connect(address, port)
            repeat(numberOfPackages) { seq ->
                var data = seq.toLong().toByteArray(4)
                data += System.nanoTime().toByteArray(8)
                data += Random.nextBytes(Settings.packetSize - data.size)
                sender.send(data)
            }
        } catch (e: IOException) {
            controller.showNetworkingError(e.message!!)
        } finally {
            sender.disconnect()
            controller.sendingFinished()
        }
    }

    private fun setProtocol(protocol: Protocol) {
        this.protocol = protocol
    }

    private fun subscribeOnEvents() {
        observerThread = thread {
            try {
                runBlocking {
                    controller.eventFlow.cancellable().collect { event ->
                        when (event) {
                            is ClientController.SendEvent ->
                                send(event.address, event.port, event.numberOfPackages)
                            is ClientController.SetProtocolEvent ->
                                setProtocol(event.protocol)
                        }
                    }
                }
            } catch (_: InterruptedException) {}
        }
    }

    override fun start() {
        subscribeOnEvents()
    }

    override fun stop() {
        observerThread.stopThread()
        sockets.forEach { (_, sender) ->
            sender.close()
        }
    }
}
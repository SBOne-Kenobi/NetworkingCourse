package model

import domain.Copy
import control.CopyCounterController
import Settings
import java.net.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask
import kotlin.math.ceil

class CopyCounterModel(
    private val controller: CopyCounterController,
    private val broadcastAddress: InetAddress,
    private val broadcastPort: Int,
    timeout: Int
) {
    private val broadcastSocket = DatagramSocket(null).apply {
        reuseAddress = true
        bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), broadcastPort))
    }
    private val receiveBroadcastBuffer = ByteArray(broadcastSocket.receiveBufferSize)
    private val receiveBroadcastPacket = DatagramPacket(receiveBroadcastBuffer, receiveBroadcastBuffer.size)

    private val appSocket = DatagramSocket().apply {
        broadcast = true
    }
    private val receiveAppBuffer = ByteArray(appSocket.receiveBufferSize)
    private val receiveAppPacket = DatagramPacket(receiveAppBuffer, receiveAppBuffer.size)

    private val prefix = "CopyCounter"
    private val currentPid = ProcessHandle.current().pid()

    private lateinit var receiveBroadcastThread: Thread
    private lateinit var receiveAppThread: Thread
    private var pingTimer: Timer? = null
    private var pingTask: TimerTask? = null
    private var checkAliveTask: TimerTask? = null

    private val _timeout = AtomicInteger(timeout)
    var timeout: Int
        get() = _timeout.get()
        set(value) {
            _timeout.set(value)
            setupTimerTasks()
        }

    private val aliveCopies = ConcurrentHashMap<Copy, Long>()

    private fun send(address: InetAddress, port: Int, command: Command) {
        val data =
            "${prefix}@${currentPid}@${command}".toByteArray()
        val packet = DatagramPacket(data, data.size, address, port)
        appSocket.send(packet)
    }

    private fun broadcast(command: Command) {
        send(broadcastAddress, broadcastPort, command)
    }

    private fun register(copy: Copy) {
        aliveCopies[copy] = System.currentTimeMillis()
        controller.register(copy)
        send(copy.address, copy.port, Command.PING)
    }

    private fun update(copy: Copy) {
        if (!aliveCopies.containsKey(copy))
            register(copy)
        else
            aliveCopies[copy] = System.currentTimeMillis()
    }

    private fun unregister(copy: Copy) {
        aliveCopies.remove(copy)
        controller.unregister(copy)
    }

    private fun receive(socket: DatagramSocket, packet: DatagramPacket) {
        socket.receive(packet)
        val (copy, command) = packet.data.decodeToString(endIndex = packet.length).split("@")
            .let {
                if (it.size != 3) error("Wrong format")
                if (it[0] != prefix) error("Wrong prefix")
                Copy(
                    packet.address,
                    packet.port,
                    it[1].toLong()
                ) to Command.valueOf(it.last())
            }
        when (command) {
            Command.START -> register(copy)
            Command.PING -> update(copy)
            Command.STOP -> unregister(copy)
        }
    }

    private fun initReceiveThread(socket: DatagramSocket, packet: DatagramPacket) = thread {
        while (!Thread.interrupted()) {
            try {
                receive(socket, packet)
            } catch (_: SocketTimeoutException) {
            } catch (e: Exception) {
                System.err.println(e)
            }
            socket.soTimeout = timeout
        }
    }

    private fun setupTimerTasks() {
        pingTimer?.apply {
            pingTask?.cancel()
            checkAliveTask?.cancel()
            purge()
            pingTask = timerTask {
                broadcast(Command.PING)
            }
            val tm = timeout
            checkAliveTask = timerTask {
                val current = System.currentTimeMillis()
                aliveCopies.toList().forEach { (copy, last) ->
                    if (current - last > tm) {
                        unregister(copy)
                    }
                }
            }
            val pingTime = ceil(timeout.toDouble() / Settings.pingTimeoutIntervalNumber).toLong()
            val checkTime = (timeout.toLong() + 1) / 2
            schedule(pingTask, 0, pingTime)
            schedule(checkAliveTask, checkTime, checkTime)
        }
    }

    fun start() {
        broadcast(Command.START)
        receiveBroadcastThread = initReceiveThread(broadcastSocket, receiveBroadcastPacket)
        receiveAppThread = initReceiveThread(appSocket, receiveAppPacket)
        pingTimer = Timer()
        setupTimerTasks()
    }

    private fun stopThread(thread: Thread) {
        thread.interrupt()
        thread.join(Settings.waitToStopThreadMillis)
    }

    fun stop() {
        pingTask?.cancel()
        checkAliveTask?.cancel()
        pingTimer?.apply {
            purge()
            cancel()
        }
        stopThread(receiveBroadcastThread)
        stopThread(receiveAppThread)
        broadcast(Command.STOP)
    }

    enum class Command {
        START,
        PING,
        STOP,
    }

}
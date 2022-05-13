package rip

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress

data class RIPData(
    val command: RIP.Command,
    val destination: InetAddress,
    val nextHop: InetAddress,
    val cost: Int
)

object RIP {

    private object CommandStorage {
        private val commandByNumber = mutableMapOf<Number, Command>()

        fun register(command: Command) {
            commandByNumber[command.number] = command
        }

        fun getByNumber(number: Int) =
            commandByNumber[number]!!
    }

    enum class Command(val number: Int) {
        REQUEST(1),
        RESPONSE(2);

        companion object {
            fun getByNumber(number: Int) =
                CommandStorage.getByNumber(number)
        }

        init {
            CommandStorage.register(this)
        }
    }

    private const val VERSION = 2
    private const val AF_INET = 2
    private const val ROUTE_TAG = 0
    private val SUBNET_MASK = ByteArray(4) { 0xFF.toByte() }

    fun buildPackage(data: RIPData): ByteArray {
        val output = ByteArrayOutputStream(24)
        output.write(data.command.number)
        output.write(VERSION)
        output.writeBytes(0.toByteArray(2))

        output.writeBytes(AF_INET.toByteArray(2))
        output.writeBytes(ROUTE_TAG.toByteArray(2))
        output.writeBytes(data.destination.address)
        output.writeBytes(SUBNET_MASK)
        output.writeBytes(data.nextHop.address)
        output.writeBytes(data.cost.toByteArray(4))

        return output.toByteArray()
    }

    fun parsePackage(data: ByteArray): RIPData {
        val input = ByteArrayInputStream(data)
        val command = Command.getByNumber(input.read())
        assert(input.read() == VERSION)
        assert(input.readNBytes(2).toInt() == 0)

        assert(input.readNBytes(2).toInt() == AF_INET)
        assert(input.readNBytes(2).toInt() == ROUTE_TAG)
        val address = InetAddress.getByAddress(input.readNBytes(4))
        assert(input.readNBytes(4).contentEquals(SUBNET_MASK))
        val nextHop = InetAddress.getByAddress(input.readNBytes(4))
        val cost = input.readNBytes(4).toInt()

        return RIPData(command, address, nextHop, cost)
    }

}

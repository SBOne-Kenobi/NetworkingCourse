package localdetection.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import localdetection.Settings
import localdetection.closeHandle
import localdetection.control.MainController
import localdetection.toByteArray
import localdetection.toInt
import org.pcap4j.core.*
import org.pcap4j.packet.ArpPacket
import org.pcap4j.packet.EthernetPacket
import org.pcap4j.packet.namednumber.ArpHardwareType
import org.pcap4j.packet.namednumber.ArpOperation
import org.pcap4j.packet.namednumber.EtherType
import org.pcap4j.util.ByteArrays
import org.pcap4j.util.MacAddress
import java.net.InetAddress
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class MainModel(
    controller: MainController,
) {
    init {
        controller.model = this
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val cancelDetection = AtomicBoolean(false)

    private val localAddress: PcapAddress by lazy {
        Pcaps.findAllDevs()
            .flatMap {
                it.addresses
            }
            .first {
                it.address.hostAddress.startsWith("192.168")
            }
    }

    private val networkInterface: PcapNetworkInterface by lazy {
        Pcaps.getDevByAddress(
            localAddress.address
        )
    }

    private val localMacAddress: MacAddress by lazy {
        networkInterface.linkLayerAddresses.first() as MacAddress
    }

    private val localDevice: Device by lazy {
        Device(
            localAddress.address,
            localMacAddress.address,
            kotlin.runCatching { localAddress.address.hostName }.getOrNull()
        )
    }

    private val networkMask: Int by lazy {
        localAddress.netmask.address.toInt()
    }

    private val addressesInSubmask: Int by lazy {
        val zeros = networkMask.countTrailingZeroBits()
        1 shl zeros
    }

    private fun getFilter(address: InetAddress): String {
        return "arp and src host %s and dst host %s".format(
            address.hostAddress,
            localAddress.address.hostAddress,
            Pcaps.toBpfString(localMacAddress)
        )
    }

    private fun getHandle(): PcapHandle {
        return networkInterface.openLive(
            Settings.snapLen,
            PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
            Settings.readTimeout
        )
    }

    private fun getPacketBuilder(address: InetAddress): EthernetPacket.Builder {
        val arpBuilder = ArpPacket.Builder().apply {
            hardwareType(ArpHardwareType.ETHERNET)
            protocolType(EtherType.IPV4)
            hardwareAddrLength(MacAddress.SIZE_IN_BYTES.toByte())
            protocolAddrLength(ByteArrays.INET4_ADDRESS_SIZE_IN_BYTES.toByte())
            srcHardwareAddr(localMacAddress)
            srcProtocolAddr(localAddress.address)
            dstHardwareAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
            dstProtocolAddr(address)
            operation(ArpOperation.REQUEST)
        }
        return EthernetPacket.Builder().apply {
            dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
            srcAddr(localMacAddress)
            type(EtherType.ARP)
            payloadBuilder(arpBuilder)
            paddingAtBuild(true)
        }
    }

    private fun tryGetMacAddress(address: InetAddress): MacAddress? {
        val remoteMacAddress = AtomicReference<MacAddress?>(null)

        val receiveHandle = getHandle()
        receiveHandle.setFilter(getFilter(address), BpfProgram.BpfCompileMode.OPTIMIZE)
        val sendHandle = getHandle()

        val packet = getPacketBuilder(address).build()
        val cyclicBarrier = CyclicBarrier(2)
        val receiver = executor.submit {
            cyclicBarrier.await()
            receiveHandle.loop(1) { reply ->
                if (reply.contains(ArpPacket::class.java)) {
                    val arp = reply.get(ArpPacket::class.java)
                    if (arp.header.operation == ArpOperation.REPLY) {
                        remoteMacAddress.set(arp.header.srcHardwareAddr)
                    }
                }
            }
        }

        return try {
            sendHandle.sendPacket(packet)
            cyclicBarrier.await()
            receiver.get(Settings.connectionTimeout, TimeUnit.MILLISECONDS)
            remoteMacAddress.get()
        } catch (e: Exception) {
            null
        } finally {
            sendHandle.closeHandle()
            receiveHandle.closeHandle()
            receiver.cancel(true)
        }
    }

    private fun tryGetDevice(address: ByteArray): Device? {
        val inetAddress = InetAddress.getByAddress(address)
        val macAddress = tryGetMacAddress(inetAddress) ?: return null
        return Device(
            inetAddress,
            macAddress.address,
            null
        )
    }


    fun detect(): Flow<MainController.Event> {
        return channelFlow {
            send(MainController.StartEvent(max(addressesInSubmask - 2, 1)))
            send(MainController.CurrentDevice(localDevice))
            val localAddressInt = localAddress.address.address.toInt()
            for (postfix in 1 until addressesInSubmask - 1) {
                if (cancelDetection.get()) {
                    cancelDetection.set(false)
                    break
                }
                val address = localAddressInt.and(networkMask).or(postfix)
                if (address == localAddressInt) continue
                tryGetDevice(address.toByteArray())?.let { device ->
                    send(MainController.NewDevice(device))
                } ?: send(MainController.EmptyIter)
            }
        }.flowOn(Dispatchers.IO).cancellable()
    }

    fun cancelDetection() {
        cancelDetection.set(true)
    }

    fun start() {
    }

    fun stop() {
    }

}
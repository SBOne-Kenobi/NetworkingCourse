import control.CopyCounterController
import model.CopyCounterModel
import view.CopyCounterView
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.streams.toList

private fun getBroadcastAddress(): InetAddress {
    return if (Settings.useLocalBroadcast) {
        InetAddress.getByName("255.255.255.255")
    } else {
        NetworkInterface
            .networkInterfaces()
            .toList()
            .filter {
                it.isUp && !it.isLoopback
            }
            .flatMap {
                it.interfaceAddresses
            }
            .first {
                it.address.hostAddress.startsWith("192.168")
            }.broadcast
    }
}

fun main() {
    val controller = CopyCounterController()
    val view = CopyCounterView(controller)
    val model = CopyCounterModel(
        controller,
        getBroadcastAddress(),
        Settings.broadcastPort,
        Settings.defaultTimeout
    )

    controller.model = model
    controller.view = view
    controller.run()
}
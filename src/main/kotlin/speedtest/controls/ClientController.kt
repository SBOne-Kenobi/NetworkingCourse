package speedtest.controls

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import speedtest.networking.ClientNetworking
import speedtest.views.ClientView
import java.net.InetAddress

class ClientController : BaseController<ClientView, ClientNetworking> {
    override lateinit var view: ClientView
    override lateinit var networking: ClientNetworking

    private val eventChannel = Channel<Event>(Channel.UNLIMITED)
    val eventFlow = eventChannel.receiveAsFlow()

    fun send(address: InetAddress, port: Int, numberOfPackages: Int) {
        eventChannel.trySend(SendEvent(address, port, numberOfPackages))
    }

    fun setProtocol(protocol: Protocol) {
        eventChannel.trySend(SetProtocolEvent(protocol))
    }

    fun showNetworkingError(message: String) {
        view.showError("Networking error", message)
    }

    fun sendingFinished() {
        view.sendingFinished()
    }

    sealed interface Event
    data class SendEvent(val address: InetAddress, val port: Int, val numberOfPackages: Int) : Event
    data class SetProtocolEvent(val protocol: Protocol) : Event
}
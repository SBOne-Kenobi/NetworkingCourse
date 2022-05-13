package speedtest.controls

import kotlinx.coroutines.flow.Flow
import speedtest.networking.ServerNetworking
import speedtest.views.ServerView
import java.net.InetAddress

class ServerController : BaseController<ServerView, ServerNetworking> {
    override lateinit var view: ServerView
    override lateinit var networking: ServerNetworking

    fun receive(address: InetAddress, port: Int) : Flow<Triple<Int, Int, Float>> {
        return networking.receive(address, port)
    }

    fun setProtocol(protocol: Protocol) {
        networking.setProtocol(protocol)
    }

    fun showNetworkingError(message: String) {
        view.showError("Networking error", message)
    }

}
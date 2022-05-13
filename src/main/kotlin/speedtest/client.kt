package speedtest

import speedtest.controls.ClientController
import speedtest.networking.ClientNetworking
import speedtest.views.ClientView

fun main() {
    val controller = ClientController()
    val view = ClientView(controller)
    val networking = ClientNetworking(controller)

    controller.run()
}
package speedtest

import speedtest.controls.ServerController
import speedtest.networking.ServerNetworking
import speedtest.views.ServerView

fun main() {
    val controller = ServerController()
    val view = ServerView(controller)
    val networking = ServerNetworking(controller)

    controller.run()
}
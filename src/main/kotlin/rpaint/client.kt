package rpaint

import rpaint.control.PaintClientController
import rpaint.networking.PaintClientNetworking
import rpaint.view.PaintClientView

fun main() {
    val controller = PaintClientController()
    val view = PaintClientView(controller)
    val networking = PaintClientNetworking(controller)

    controller.run()
}
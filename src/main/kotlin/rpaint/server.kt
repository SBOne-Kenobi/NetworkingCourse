package rpaint

import rpaint.control.PaintServerController
import rpaint.networking.PaintServerNetworking
import rpaint.view.PaintServerView

fun main() {
    val controller = PaintServerController()
    val view = PaintServerView(controller)
    val networking = PaintServerNetworking(controller)

    controller.run()
}
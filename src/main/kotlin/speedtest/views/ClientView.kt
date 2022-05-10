package speedtest.views

import speedtest.controls.ClientController
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class ClientView(
    private val controller: ClientController
) : BaseView {

    init {
        controller.view = this
    }

    lateinit var frame: ClientFrame

    fun showError(title: String, message: String) {
        runOnUI {
            frame.createDialog(title, message, 2000)
        }
    }

    fun sendingFinished() {
        runOnUI {
            frame.sendingFinished()
        }
    }

    override fun run(onExit: () -> Unit) {
        runOnUI {
            frame = ClientFrame(controller)
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    super.windowClosing(e)
                    onExit()
                }
            })
            frame.isVisible = true
        }
    }
}
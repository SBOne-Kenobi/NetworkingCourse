package speedtest.views

import speedtest.controls.ServerController
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class ServerView(
    private val controller: ServerController
) : BaseView {

    init {
        controller.view = this
    }

    private lateinit var frame: ServerFrame

    fun showError(title: String, message: String) {
        runOnUI {
            frame.createDialog(title, message, 2000)
        }
    }

    override fun run(onExit: () -> Unit) {
        runOnUI {
            frame = ServerFrame(controller)
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
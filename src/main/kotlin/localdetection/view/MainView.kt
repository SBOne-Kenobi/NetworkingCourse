package localdetection.view

import localdetection.control.MainController
import java.awt.EventQueue
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class MainView(
    private val controller: MainController,
) {
    init {
        controller.view = this
    }

    private fun runOnUI(block: () -> Unit) {
        EventQueue.invokeLater(block)
    }

    private val frame: MainFrame by lazy {
        MainFrame(controller)
    }

    fun run(onExit: () -> Unit) {
        runOnUI {
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    super.windowClosing(e)
                    frame.close()
                    onExit()
                }
            })
            frame.isVisible = true
        }
    }

}
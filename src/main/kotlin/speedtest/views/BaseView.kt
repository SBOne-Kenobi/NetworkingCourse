package speedtest.views

import java.awt.EventQueue

interface BaseView {
    fun run(onExit: () -> Unit)

    fun runOnUI(block: () -> Unit) {
        EventQueue.invokeLater(block)
    }
}
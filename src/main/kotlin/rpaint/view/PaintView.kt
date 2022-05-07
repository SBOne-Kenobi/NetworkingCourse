package rpaint.view

import java.awt.EventQueue

interface PaintView {
    fun run(onExit: () -> Unit)

    fun runOnUI(block: () -> Unit) {
        EventQueue.invokeLater(block)
    }
}
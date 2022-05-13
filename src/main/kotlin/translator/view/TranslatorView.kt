package translator.view

import translator.control.TranslatorController
import java.awt.EventQueue
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JOptionPane

class TranslatorView(
    private val controller: TranslatorController
) {

    init {
        controller.view = this
    }

    fun runOnUI(block: () -> Unit) {
        EventQueue.invokeLater(block)
    }

    private lateinit var frame: TranslatorFrame

    fun run(onExit: () -> Unit) {
        runOnUI {
            frame = TranslatorFrame(controller)
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
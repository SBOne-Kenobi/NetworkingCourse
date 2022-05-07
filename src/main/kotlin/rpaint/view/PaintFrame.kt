package rpaint.view

import rpaint.Settings
import java.awt.BorderLayout
import javax.swing.JFrame

class PaintFrame(
    title: String = "Paint"
) : JFrame(title) {
    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(Settings.windowWidth, Settings.windowHeight)
        setLocationRelativeTo(null)
        isResizable = false
    }

    val drawArea = DrawArea()

    init {
        val content = contentPane
        content.layout = BorderLayout()
        add(drawArea, BorderLayout.CENTER)
    }

}
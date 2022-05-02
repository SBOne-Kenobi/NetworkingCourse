package rpaint.view

import rpaint.control.PaintClientController
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.*
import javax.swing.JButton
import javax.swing.JPanel

class PaintClientView(
    private val controller: PaintClientController
) : PaintView {

    init {
        controller.view = this
    }

    private lateinit var frame: PaintFrame

    private var oldPoint = Point(0, 0)
    private var currentPoint = Point(0, 0)

    private fun initDrawArea(drawArea: DrawArea) {
        drawArea.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                oldPoint = e.point
            }
        })
        drawArea.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                currentPoint = e.point
                drawArea.drawLine(oldPoint, currentPoint)
                controller.drawLine(oldPoint, currentPoint)
                oldPoint = currentPoint
            }
        })
    }

    private fun buildControls() : JPanel {
        val controls = JPanel()

        val clearButton = JButton("clear").apply {
            addActionListener {
                frame.drawArea.clear()
                controller.clear()
            }
        }

        controls.add(clearButton)
        return controls
    }

    override fun run(onExit: () -> Unit) {
        runOnUI {
            frame = PaintFrame("Paint Client")

            val drawArea = frame.drawArea
            initDrawArea(drawArea)

            val controls = buildControls()
            frame.contentPane.add(controls, BorderLayout.NORTH)

            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent?) {
                    super.windowClosed(e)
                    onExit()
                }
            })

            frame.isVisible = true
        }
    }

}
package rpaint.view

import rpaint.control.PaintClientController
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import java.awt.event.*
import javax.swing.JButton
import javax.swing.JLabel
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

    private fun colorActionListener(currentColor: JLabel, color: Color) =
        ActionListener {
            frame.drawArea.color = color
            currentColor.background = color
            controller.setColor(color)
        }

    private fun buildControls() : JPanel {
        val controls = JPanel()

        val currentColor = JLabel().apply {
            background = frame.drawArea.color
            isOpaque = true
            preferredSize = Dimension(15, 15)
        }
        val redButton = JButton("red").apply {
            addActionListener(colorActionListener(currentColor, Color.red))
        }
        val blackButton = JButton("black").apply {
            addActionListener(colorActionListener(currentColor, Color.black))
        }
        val blueButton = JButton("blue").apply {
            addActionListener(colorActionListener(currentColor, Color.blue))
        }
        val greenButton = JButton("green").apply {
            addActionListener(colorActionListener(currentColor, Color.green))
        }
        val clearButton = JButton("clear").apply {
            addActionListener {
                frame.drawArea.clear()
                controller.clear()
            }
        }

        controls.add(currentColor)
        controls.add(blackButton)
        controls.add(redButton)
        controls.add(greenButton)
        controls.add(blueButton)
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
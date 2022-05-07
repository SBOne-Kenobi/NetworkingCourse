package rpaint.view

import rpaint.control.PaintServerController
import java.awt.Color
import java.awt.Point
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class PaintServerView(
    controller: PaintServerController
) : PaintView {

    init {
        controller.view = this
    }

    private lateinit var frame: PaintFrame

    fun drawLine(oldPoint: Point, newPoint: Point) {
        runOnUI {
            frame.drawArea.drawLine(oldPoint, newPoint)
        }
    }

    fun clear() {
        runOnUI {
            frame.drawArea.clear()
        }
    }

    fun setColor(color: Color) {
        runOnUI {
            frame.drawArea.color = color
        }
    }

    override fun run(onExit: () -> Unit) {
        runOnUI {
            frame = PaintFrame("Paint Server")

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
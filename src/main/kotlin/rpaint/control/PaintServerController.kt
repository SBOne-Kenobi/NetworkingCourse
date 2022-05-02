package rpaint.control

import rpaint.networking.PaintServerNetworking
import rpaint.view.PaintServerView
import java.awt.Color
import java.awt.Point

class PaintServerController : PaintController<PaintServerView, PaintServerNetworking> {
    override lateinit var view: PaintServerView
    override lateinit var networking: PaintServerNetworking

    override fun drawLine(oldPoint: Point, newPoint: Point) {
        view.drawLine(oldPoint, newPoint)
    }

    override fun setColor(color: Color) {
        view.setColor(color)
    }

    override fun clear() {
        view.clear()
    }
}
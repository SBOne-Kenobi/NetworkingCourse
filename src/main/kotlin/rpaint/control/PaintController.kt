package rpaint.control

import rpaint.networking.PaintNetworking
import rpaint.view.PaintView
import java.awt.Color
import java.awt.Point

interface PaintController<PV: PaintView, PN: PaintNetworking> {

    var view: PV
    var networking: PN

    fun run() {
        networking.start()
        view.run {
            networking.stop()
        }
    }

    fun drawLine(oldPoint: Point, newPoint: Point)

    fun setColor(color: Color)

    fun clear()

}
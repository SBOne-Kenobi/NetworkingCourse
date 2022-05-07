package rpaint.view

import java.awt.*
import java.awt.geom.Line2D
import javax.swing.JComponent


class DrawArea() : JComponent() {
    private var image: Image? = null
    private var g2: Graphics2D? = null

    var color: Color = Color.black
        set(value) {
            g2?.let {
                it.paint = value
                field = value
            }
        }

    init {
        isDoubleBuffered = false
    }

    fun drawLine(oldPoint: Point, newPoint: Point) {
        g2?.apply {
            draw(Line2D.Double(oldPoint, newPoint))
            repaint()
        }
    }

    override fun paintComponent(g: Graphics) {
        if (image == null) {
            image = createImage(size.width, size.height)
            g2 = image?.graphics as Graphics2D?
            g2?.apply {
                setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                )
                stroke = BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            }
            clear()
        }
        g.drawImage(image, 0, 0, null)
    }

    fun clear() {
        g2?.let {
            it.paint = Color.white
            it.fillRect(0, 0, size.width, size.height)
            it.paint = color
            repaint()
        }
    }
}
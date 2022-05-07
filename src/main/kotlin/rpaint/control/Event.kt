package rpaint.control

import java.awt.Color
import java.awt.Point

sealed interface Event
data class DrawLine(val oldPoint: Point, val newPoint: Point) : Event
data class SetColor(val color: Color) : Event
object Clear : Event

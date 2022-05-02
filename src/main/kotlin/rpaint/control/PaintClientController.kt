package rpaint.control

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import rpaint.networking.PaintClientNetworking
import rpaint.view.PaintClientView
import java.awt.Color
import java.awt.Point

class PaintClientController : PaintController<PaintClientView, PaintClientNetworking> {

    override lateinit var view: PaintClientView
    override lateinit var networking: PaintClientNetworking

    private val eventChannel = Channel<Event>(Channel.UNLIMITED)
    val eventFlow = eventChannel.receiveAsFlow()

    override fun drawLine(oldPoint: Point, newPoint: Point) {
        eventChannel.trySend(DrawLine(oldPoint, newPoint))
    }

    override fun setColor(color: Color) {
        eventChannel.trySend(SetColor(color))
    }

    override fun clear() {
        eventChannel.trySend(Clear)
    }

}
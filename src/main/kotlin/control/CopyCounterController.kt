package control

import domain.Copy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import model.CopyCounterModel
import view.CopyCounterView

class CopyCounterController {
    lateinit var model: CopyCounterModel
    lateinit var view: CopyCounterView

    private val channel = Channel<Event>(Channel.UNLIMITED)
    val eventFlow
        get() = channel.receiveAsFlow()

    fun register(copy: Copy) {
        channel.trySend(Event.NewCopy(copy))
    }

    fun unregister(copy: Copy) {
        channel.trySend(Event.RemoveCopy(copy))
    }

    var timeout: Int
        get() = model.timeout
        set(value) {
            model.timeout = value
        }

    fun run() {
        model.start()
        view.run(::stop)
    }

    private fun stop() {
        view.stop()
        model.stop()
        channel.close()
        channel.cancel()
    }

    sealed class Event {
        data class NewCopy(val copy: Copy) : Event()
        data class RemoveCopy(val copy: Copy) : Event()
    }
}
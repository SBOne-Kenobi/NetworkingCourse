package localdetection.control

import kotlinx.coroutines.flow.Flow
import localdetection.model.Device
import localdetection.model.MainModel
import localdetection.view.MainView

class MainController {
    lateinit var view: MainView
    lateinit var model: MainModel

    fun run() {
        model.start()
        view.run {
            model.stop()
        }
    }

    fun detect(): Flow<Event> {
        return model.detect()
    }

    fun cancelDetection() {
        model.cancelDetection()
    }

    sealed interface Event
    data class StartEvent(val totalAddresses: Int) : Event
    data class NewDevice(val device: Device) : Event
    data class CurrentDevice(val device: Device) : Event
    object EmptyIter : Event

}
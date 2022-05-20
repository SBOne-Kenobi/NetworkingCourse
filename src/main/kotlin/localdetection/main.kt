package localdetection

import localdetection.control.MainController
import localdetection.model.MainModel
import localdetection.view.MainView

fun main() {
    val controller = MainController()
    MainView(controller)
    MainModel(controller)

    controller.run()
}
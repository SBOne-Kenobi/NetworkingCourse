package speedtest.controls

import speedtest.networking.BaseNetworking
import speedtest.views.BaseView

interface BaseController<V: BaseView, N: BaseNetworking> {
    var view: V
    var networking: N

    fun run() {
        networking.start()
        view.run {
            networking.stop()
        }
    }
}
package translator

object Settings {
    const val windowWidth = 500
    const val windowHeight = 500

    const val timeoutToStopThread: Long = 500
}

fun Thread.stopThread() {
    interrupt()
    join(Settings.timeoutToStopThread)
}

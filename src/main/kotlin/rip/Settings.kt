package rip

object Settings {
    const val timeoutToStopThread: Long = 3000
    const val timeoutToFit: Long = 500

    // creates delay on sending in communication
    const val allowDelayCommunication: Boolean = false
    const val delayCommunication: Long = 10

    const val updateTimer: Long = 10
//    const val invalidTimer: Long = 60
//    const val flushTimer: Long = invalidTimer + 2 * updateTimer
}

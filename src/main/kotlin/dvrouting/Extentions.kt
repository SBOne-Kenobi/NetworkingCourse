package dvrouting

import java.util.concurrent.locks.Lock

fun Thread.stopThread() {
    interrupt()
    join(Settings.timeoutToStopThread)
}

inline fun <T> Lock.withInterruptiblyLock(block: () -> T): T {
    try {
        lockInterruptibly()
        return block()
    } finally {
        unlock()
    }
}

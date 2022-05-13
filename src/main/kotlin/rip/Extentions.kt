package rip

import java.net.InetAddress
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

fun Int.toByteArray(len: Int): ByteArray =
    ByteArray(len) {
        shr(it * 8).toByte()
    }

fun ByteArray.toInt(): Int =
    foldIndexed(0) { i, acc, byte ->
        acc + byte.toUByte().toInt().shl(i * 8)
    }

val InetAddress.node: Node
    get() = Node.getByAddress(this)

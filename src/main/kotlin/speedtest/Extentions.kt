package speedtest

fun Thread.stopThread() {
    interrupt()
    join(Settings.timeoutToStopThread)
}

fun ByteArray.toLong(): Long {
    return foldIndexed(0L) { i, acc, byte ->
        acc + byte.toUByte().toLong().shl(i * 8)
    }
}

fun Long.toByteArray(len: Int) : ByteArray {
    return ByteArray(len) {
        shr(it * 8).toByte()
    }
}

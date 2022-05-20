package localdetection

import org.pcap4j.core.PcapHandle

fun Thread.stopThread() {
    interrupt()
    join(Settings.timeoutToThreadStop)
    @Suppress("DEPRECATION")
    if (isAlive) stop()
}

fun PcapHandle.closeHandle() {
    try {
        breakLoop()
    } catch (_: Exception) {}
    try {
        close()
    } catch (_: Exception) {}
}

fun ByteArray.toInt(): Int {
    return fold(0) { acc, byte ->
        (acc shl Byte.SIZE_BITS).or(byte.toUByte().toInt())
    }
}

fun Int.toByteArray(): ByteArray {
    return ByteArray(Int.SIZE_BYTES) {
        shr(Int.SIZE_BITS - (it + 1) * Byte.SIZE_BITS).and(UByte.MAX_VALUE.toInt()).toByte()
    }
}
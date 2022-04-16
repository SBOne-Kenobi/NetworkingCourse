package protocol

import debug

fun computeChecksum(bytes: ByteArray, reverse: Boolean = true): UShort {
    var result = 0u
    var i = 0
    while (i < bytes.size) {
        val firstByte = bytes[i].toUInt()
        val secondByte = bytes.getOrNull(i + 1)?.toUInt() ?: 0u
        val word = (firstByte shl 8) + secondByte
        result += word
        i += 2
    }
    return result.toUShort().let {
        if (reverse)
            it.xor(UShort.MAX_VALUE)
        else
            it
    }.also {
        debug("Checksum ($reverse): $it")
    }
}

fun validateChecksum(bytes: ByteArray, checksum: UShort): Boolean {
    debug("Received checksum: $checksum")
    return (computeChecksum(bytes, false) + checksum).toUShort() == UShort.MAX_VALUE
}
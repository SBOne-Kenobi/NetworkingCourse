package crc

import java.io.InputStream

const val DEFAULT_BASE = 0x84C11DB7.toInt()
fun crc32(input: InputStream, base: Int = DEFAULT_BASE): Int {
    var result = 0
    val bitReader = BitReader(input)

    while (true) {
        val bit = bitReader.next() ?: break
        result = result shl 1
        if (bit) result += 1
        if (result.and(1 shl (Int.SIZE_BITS - 1)) != 0) {
            result = result.xor(base)
        }
    }
    return result
}

fun crc32Check(input: InputStream, value: Int, base: Int = DEFAULT_BASE): Boolean =
    crc32(input, base) == value

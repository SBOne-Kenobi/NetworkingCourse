package crc

import java.io.InputStream
import kotlin.experimental.and

class BitReader(private val input: InputStream) {
    private val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    private var size = 0
    private var read = 0

    private val byteIdx: Int
        get() = read.div(Byte.SIZE_BITS)

    private val offset: Int
        get() = read.mod(Byte.SIZE_BITS)

    fun next(): Boolean? {
        if (read >= size) {
            size = input.read(buffer) * Byte.SIZE_BITS
            read = 0
            if (size <= 0)
                return null
        }
        return (buffer[byteIdx].and((1 shl offset).toByte()) != 0.toByte()).also {
            read += 1
        }
    }
}
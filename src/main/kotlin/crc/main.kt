package crc

import kotlin.experimental.xor
import kotlin.random.Random

const val chunkSize = 5

fun makeNoise(data: ByteArray, noiseProbability: Double) {
    if (Random.nextDouble() < noiseProbability) {
        val idx = Random.nextInt(0, data.size)
        val offset = Random.nextInt(0, Byte.SIZE_BITS)
        data[idx] = data[idx].xor((1 shl offset).toByte())
    }
}

fun <R> String.runOnChunked(block: (ByteArray) -> R): List<R> =
    toByteArray().toList().chunked(chunkSize) {
        block(it.toByteArray())
    }

fun test(text: String, noiseProbability: Double) {
    val separator = "------------------------------------"
    println("Test on: $text")
    println(separator)
    text.runOnChunked { data ->
        println("Data: ${data.decodeToString()}")
        val crc = crc32(data.inputStream())
        println("CRC32: ${crc.toUInt().toString(16)}")
        makeNoise(data, noiseProbability)
        println("Data with noise: ${data.decodeToString()}")
        println("CRC validation: ${crc32Check(data.inputStream(), crc)}")
        println(separator)
    }
}

fun main() {
    test("I have an apple.", 0.4)
    test("Text with noise", 1.0)
    test("Text without noise", 0.0)
}

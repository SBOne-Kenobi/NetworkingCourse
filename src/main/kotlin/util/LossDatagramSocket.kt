package util

import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.properties.Delegates
import kotlin.random.Random

class LossDatagramSocket : DatagramSocket {
    private var lossProbability by Delegates.notNull<Float>()
    private lateinit var random: Random

    private fun isLoss(): Boolean {
        return random.nextFloat() < lossProbability
    }

    private fun init(lossProbability: Float, seed: Long?) {
        this.lossProbability = lossProbability
        this.random = seed?.let { Random(it) } ?: Random
    }

    constructor(lossProbability: Float, seed: Long? = null) : super() {
        init(lossProbability, seed)
    }

    constructor(port: Int, lossProbability: Float, seed: Long? = null) : super(port) {
        init(lossProbability, seed)
    }

    override fun send(p: DatagramPacket) {
        if (!isLoss()) super.send(p)
    }
}
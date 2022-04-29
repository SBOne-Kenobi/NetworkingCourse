import protocol.GBNReceiver
import protocol.GBNSender
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

object State {

    private val lock = ReentrantLock()

    private var clientBaseNumber = 0
    private var clientNextNumber = 0
    private var serverNextNumber = 0
    var numberOfFrames = 0

    private fun printState() {
        val clientEndNumber = min(clientBaseNumber + Settings.windowSize, numberOfFrames)
        println(buildString {
            append("Sender: ")
            append(buildList {
                if (clientBaseNumber == 0) add("[")
                if (clientNextNumber == 0) add("|")
                for (i in 0 until numberOfFrames) {
                    if (i == clientNextNumber - 1) add("$i|")
                    else add(i.toString())
                    if (i + 1 == clientBaseNumber) add("[")
                    if (i + 1 == clientEndNumber) add("]")
                }
            }.joinToString(" "))
            append("\n")

            append("Receiver: ")
            append(buildList {
                if (serverNextNumber == 0) add("|")
                for (i in 0 until numberOfFrames) {
                    if (i == serverNextNumber - 1) add("$i|")
                    else add(i.toString())
                }
            }.joinToString(" "))
            append("\n----------------")
        })
    }

    fun updateServer(event: GBNReceiver.Event) {
        lock.withLock {
            when(event) {
                is GBNReceiver.BadFrameEvent -> {
                    println("Receiver: received ${event.frame.frameNumber}")
                    println("Drop frame")
                }
                is GBNReceiver.NewFrameEvent -> {
                    println("Receiver: received ${event.frame.frameNumber}")
                    println("Send ack ${event.frame.frameNumber}")
                    serverNextNumber = event.frame.frameNumber + 1
                }
            }
            printState()
        }
    }

    fun updateClient(event: GBNSender.Event) {
        lock.withLock {
            when(event) {
                is GBNSender.AckReceivedEvent -> {
                    println("Sender: received ack ${event.ackFrame.frameNumber}")
                    clientBaseNumber = event.ackFrame.frameNumber
                }
                is GBNSender.FrameSentEvent -> {
                    println("Sender: frame ${event.frame.frameNumber} sent")
                    clientNextNumber = event.frame.frameNumber + 1
                }
                is GBNSender.TimeoutEvent -> {
                    println("Sender: timeout")
                    println("Resend packets ${event.first}..${event.last}")
                }
            }
            printState()
        }
    }
}
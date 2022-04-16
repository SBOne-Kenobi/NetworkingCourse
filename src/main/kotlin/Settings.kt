object Settings {
    const val lossProbability = 0.3f

    const val serverPort = 8080
    const val serverHost = "127.0.0.1"

    const val ackWaitTimeoutMillis = 1000

    const val debugOutput = false

    const val maxUPDSendSize = 65000
}

fun debug(message: String) {
    if (Settings.debugOutput)
        println(message)
}

object Settings {
    const val serverHost = "127.0.0.1"
    const val serverPort = 8080

    const val timeout: Long = 1000
    const val maxDatagramPacketSize = 7000 // can't be less than 514

    const val windowSize = 4
    const val lossProbability = 0.3f
}
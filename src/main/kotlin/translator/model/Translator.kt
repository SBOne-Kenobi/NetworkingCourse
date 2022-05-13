package translator.model

import translator.ConfigEntry
import translator.stopThread
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread

class Translator(
    private val settings: ConfigEntry,
) {
    private val serverSocket: ServerSocket = ServerSocket(settings.internal_port, 50, settings.internal_ip)

    private lateinit var translatorThread: Thread
    private val translatorExecutor = Executors.newCachedThreadPool()
    private val sessionExecutor = Executors.newCachedThreadPool()

    private fun connectExternal(): Socket {
        return if (settings.useSSL) {
            val factory = SSLSocketFactory.getDefault()
            factory.createSocket(settings.external_ip, settings.external_port).apply {
                this as SSLSocket
                startHandshake()
            }
        } else {
            Socket(settings.external_ip, settings.external_port)
        }
    }

    private fun translate(inputStream: InputStream, outputStream: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read = inputStream.read(buffer)
        while (read > 0) {
            outputStream.write(buffer, 0, read)
            outputStream.flush()
            read = inputStream.read(buffer)
        }
    }

    fun start() {
        translatorThread = thread {
            try {
                while (!Thread.interrupted()) {
                    val clientSocket = serverSocket.accept()
                    sessionExecutor.execute {
                        try {
                            val externalSocket = connectExternal()
                            val clientInput = clientSocket.getInputStream()
                            val clientOutput = clientSocket.getOutputStream()
                            val externalInput = externalSocket.getInputStream()
                            val externalOutput = externalSocket.getOutputStream()

                            val clientToExt = translatorExecutor.submit { translate(clientInput, externalOutput) }
                            val extToClient = translatorExecutor.submit { translate(externalInput, clientOutput) }
                            clientToExt.get()
                            extToClient.get()
                            clientSocket.close()
                            externalSocket.close()

                        } catch (_: InterruptedException) {
                        }
                    }
                }
            } catch (_: SocketException) {
            }
        }
    }

    fun stop() {
        translatorThread.stopThread()
        translatorExecutor.shutdownNow()
        sessionExecutor.shutdownNow()
        serverSocket.close()
    }

}
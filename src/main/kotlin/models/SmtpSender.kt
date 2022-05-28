package models

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.*
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object ContentType {
    const val PLAIN = "text/plain"
    const val HTML = "text/html"
}

object ContentDisposition {
    const val INLINE = "inline"
    const val ATTACHMENT = "attachment"
}

class SmtpSender(host: String, port: Int, useSSL: Boolean = false) : AutoCloseable {
    private val socket: Socket = if (useSSL) {
        val factory = SSLSocketFactory.getDefault()
        factory.createSocket(host, port).apply {
            this as SSLSocket
            startHandshake()
        }
    } else {
        Socket(host, port)
    }

    private val input = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val output = socket.getOutputStream()
    private val printerOutput = PrintWriter(output, true)

    private val encoder = Base64.getEncoder()

    init {
        response(checkCode(220))
        hello(host)
    }

    private fun checkCode(expectedCode: Int) = { code: Int, message: String ->
        if (code != expectedCode) {
            throw RuntimeException("Expected $expectedCode, but received $code: $message")
        }
    }

    private fun String.encode() =
        encoder.encodeToString(toByteArray())

    fun <T> response(handler: (Int, String) -> T): T {
        val response = input.readLine()
        val code = response.takeWhile { it.isDigit() }.toInt()
        val message = response.dropWhile { it.isDigit() }.trim()
        return handler(code, message)
    }

    fun send(bytes: ByteArray) {
        output.write(bytes + System.lineSeparator().toByteArray())
        output.flush()
    }

    fun send(message: String) {
        printerOutput.println(message)
    }

    fun <T> send(message: String, handler: (Int, String) -> T): T {
        send(message)
        return response(handler)
    }

    fun <T> send(bytes: ByteArray, handler: (Int, String) -> T): T {
        send(bytes)
        return response(handler)
    }

    fun hello(arg: String) {
        send("HELO $arg", checkCode(250))
    }

    fun login(email: String, password: String) {
        send("AUTH LOGIN", checkCode(334))
        send(email.encode(), checkCode(334))
        send(password.encode(), checkCode(235))
    }

    private fun sendHeader(
        from: String,
        to: String,
        subject: String,
        contentType: String
    ) {
        send("MAIL FROM: <$from>", checkCode(250))
        send("RCPT TO: <$to>", checkCode(250))
        send("DATA", checkCode(354))
        send(buildString {
            appendLine("From: $from")
            appendLine("To: $to")
            appendLine("Subject: $subject")
            append("Content-Type: $contentType")
        })
    }

    fun sendMessage(
        from: String,
        to: String,
        subject: String,
        contentType: String,
        body: String,
    ) {
        sendHeader(from, to, subject, contentType)
        send(body)
        send(".", checkCode(250))
    }

    fun sendBytes(
        from: String,
        to: String,
        subject: String,
        contentType: String,
        contentDisposition: String,
        body: ByteArray,
    ) {
        sendHeader(from, to, subject, contentType)
        send("Content-Transfer-Encoding: base64")
        send("Content-Disposition: $contentDisposition")
        send(encoder.encode(body))
        send(".", checkCode(250))
    }

    override fun close() {
        socket.close()
    }

}
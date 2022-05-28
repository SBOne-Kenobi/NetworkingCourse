package models

import Settings
import models.ContentType.PLAIN
import java.net.InetAddress
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.schedule

object UserNotifier {

    private val executor = Executors.newFixedThreadPool(10)

    private class UserInfo(
        private val user: User
    ) {
        val lastTick = AtomicLong(System.currentTimeMillis())

        private fun sendEmail() {
            executor.execute {
                try {
                    val smtpSender = SmtpSender(
                        Settings.smtpServerHost,
                        Settings.smtpServerPort,
                        Settings.smtpUseSSL
                    )
                    smtpSender.login(Settings.emailNotifierLogin, Settings.emailNotifierPassword)
                    smtpSender.sendMessage(
                        "Auth Rest Service",
                        user.email,
                        "Welcome back!",
                        PLAIN,
                        "Рады видеть вас в нашем сервисе вновь!"
                    )
                    smtpSender.close()
                    println("Email sent to user ${user.email}")
                } catch (e: Exception) {
                    System.err.println("Error while sending: ${e.message}")
                }
            }
        }

        fun checkUser(currentTime: Long): Boolean {
            return if (currentTime - lastTick.get() > Settings.notificationTimeoutMillis) {
                sendEmail()
                true
            } else {
                false
            }
        }

    }

    private val userInfos = ConcurrentHashMap<InetAddress, UserInfo>()
    private val timerNotifier = Timer()

    init {
        val delay = Settings.notificationTimeoutMillis / 3
        timerNotifier.schedule(delay, delay) {
            val currentTime = System.currentTimeMillis()
            userInfos.filterValues { info ->
                info.checkUser(currentTime)
            }.forEach { (ip, _) ->
                userInfos.remove(ip)
            }
        }
    }

    fun notify(user: User) {
        userInfos.compute(user.ipAddress) { _, info ->
            info?.apply {
                lastTick.set(System.currentTimeMillis())
            } ?: UserInfo(user)
        }
    }
}
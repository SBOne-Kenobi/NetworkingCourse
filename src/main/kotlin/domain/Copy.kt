package domain

import java.net.InetAddress

data class Copy(val address: InetAddress, val port: Int, val pid: Long) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Copy

        if (address != other.address) return false
        if (port != other.port) return false
        if (pid != other.pid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + port
        result = 31 * result + pid.hashCode()
        return result
    }
}
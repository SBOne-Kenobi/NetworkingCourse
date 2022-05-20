package localdetection.model

import java.net.InetAddress

data class Device(val ipAddress: InetAddress, val macAddress: ByteArray, val name: String? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Device

        if (ipAddress != other.ipAddress) return false
        if (!macAddress.contentEquals(other.macAddress)) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ipAddress.hashCode()
        result = 31 * result + macAddress.contentHashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }
}

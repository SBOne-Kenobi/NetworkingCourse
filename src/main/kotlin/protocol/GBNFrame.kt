package protocol

data class GBNFrame(
    val frameNumber: Int,
    val isAck: Boolean,
    val data: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GBNFrame

        if (frameNumber != other.frameNumber) return false
        if (isAck != other.isAck) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameNumber
        result = 31 * result + isAck.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}
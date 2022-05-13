package translator

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.InetAddress

class InetAddressSerializer : KSerializer<InetAddress> {
    override fun deserialize(decoder: Decoder): InetAddress {
        val address = decoder.decodeString()
        return InetAddress.getByName(address)
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("java.net.InetAddress", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InetAddress) {
        encoder.encodeString(value.hostAddress)
    }
}

class PortConfigSerializer : KSerializer<PortConfig> {
    override fun deserialize(decoder: Decoder): PortConfig {
        val entries = decoder.decodeSerializableValue(ListSerializer(ConfigEntry.serializer())).toMutableList()
        return PortConfig(entries)
    }

    override val descriptor: SerialDescriptor
        get() = serialDescriptor<List<ConfigEntry>>()

    override fun serialize(encoder: Encoder, value: PortConfig) {
        encoder.encodeSerializableValue(ListSerializer(ConfigEntry.serializer()), value.entries)
    }
}

@Serializable
data class ConfigEntry(
    val name: String,
    @Serializable(InetAddressSerializer::class)
    val internal_ip: InetAddress,
    val internal_port: Int,
    @Serializable(InetAddressSerializer::class)
    val external_ip: InetAddress,
    val external_port: Int,
    val useSSL: Boolean
)

@Serializable(PortConfigSerializer::class)
data class PortConfig(
    val entries: MutableList<ConfigEntry>
)

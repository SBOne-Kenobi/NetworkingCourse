package translator.view
import java.text.DecimalFormat
import java.text.ParseException
import javax.swing.text.DefaultFormatter
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

class IPAddressFormatter : DefaultFormatter() {

    override fun valueToString(value: Any?): String {
        if (value !is ByteArray)
            throw ParseException("Value is not a bytes", 0)
        if (value.size != 4)
            throw ParseException("Length != 4", 0)
        return value.joinToString(".") { byte ->
            byte.toUByte().toString()
        }
    }

    override fun stringToValue(string: String): Any {
        val bytes = string.split(".")
        if (bytes.size != 4)
            throw ParseException("Length != 4", 0)
        return bytes.map {
            val b = it.toIntOrNull()
                ?: throw ParseException("Not an integer", 0)
            if (b !in 0 until 256)
                throw ParseException("Byte out of range", 0)
            b.toByte()
        }.toByteArray()
    }
}

class IPAddressFormatterFactory : DefaultFormatterFactory(IPAddressFormatter())

open class IntWithLimitsFormatter(
    min: Int? = null,
    max: Int? = null
) : NumberFormatter(DecimalFormat("#")) {
    init {
        min?.let { minimum = it }
        max?.let { maximum = it }
    }
}

class IntWithLimitsFormatterFactory(
    min: Int? = null,
    max: Int? = null
) : DefaultFormatterFactory(IntWithLimitsFormatter(min, max))

class PortFormatter : IntWithLimitsFormatter(1, 65535)

class PortFormatterFactory : DefaultFormatterFactory(PortFormatter())

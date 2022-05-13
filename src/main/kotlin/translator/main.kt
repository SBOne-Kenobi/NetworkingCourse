package translator

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import translator.control.TranslatorController
import translator.model.TranslatorModel
import translator.view.TranslatorView
import java.io.File
import java.nio.file.Path

fun main(args: Array<String>) {
    val configURL = args.getOrNull(0)?.let { Path.of(it).toUri().toURL() }
        ?: ClassLoader.getSystemResource("port-config.json")
    val config = Json.decodeFromString(
        PortConfig.serializer(),
        configURL.readText()
    )
    val controller = TranslatorController(config)
    val view = TranslatorView(controller)
    val model = TranslatorModel(controller)

    controller.run {
        File(configURL.toURI()).writeText(
            JSONArray(Json.encodeToString(config)).toString(2)
        )
    }
}
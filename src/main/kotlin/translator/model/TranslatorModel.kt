package translator.model

import translator.ConfigEntry
import translator.control.TranslatorController

class TranslatorModel(
    private val controller: TranslatorController
) {

    init {
        controller.model = this
    }

    private val translators = mutableListOf<Translator>()

    fun addTranslator(settings: ConfigEntry) {
        translators.add(Translator(settings).apply { start() })
    }

    fun start() {
        controller.config.entries.forEach {
            translators.add(
                Translator(it).apply { start() }
            )
        }
    }

    fun stop() {
        translators.forEach {
            it.stop()
        }
    }

}
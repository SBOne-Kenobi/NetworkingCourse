package translator.control

import translator.ConfigEntry
import translator.PortConfig
import translator.model.TranslatorModel
import translator.view.TranslatorView

class TranslatorController(
    val config: PortConfig
) {

    lateinit var view: TranslatorView
    lateinit var model: TranslatorModel

    fun updateConfig(entry: ConfigEntry) {
        model.addTranslator(entry)
        config.entries.add(entry)
    }

    fun run(onExit: () -> Unit) {
        model.start()
        view.run {
            model.stop()
            onExit()
        }
    }

}
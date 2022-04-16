package util

class ProgressBar(private val width: Int = 20, private val char: Char = '#') {
    private var shown = 0
    private var isStarted = false

    fun start(): ProgressBar {
        println("_".repeat(width))
        isStarted = true
        return this
    }

    fun update(state: Float) {
        if (isStarted) {
            shown.let {
                shown = (width * state).toInt()
                if (shown > it) print(char.toString().repeat(shown - it))
            }
        }
    }

}
class FileHierarchy(
    val name: String,
    private val permissions: String = "-rw-rw-rw-",
    var parent: FileHierarchy? = null
) {
    val isDirectory: Boolean
        get() = permissions.startsWith('d')

    var child: List<FileHierarchy>? = null

    companion object {
        fun parse(s: String): FileHierarchy {
            val params = s.trim().split("\\s+".toRegex())
            return FileHierarchy(params[8], params[0])
        }
    }

    override fun toString(): String = buildString {
        append(name)
        if (isDirectory)
            append('/')
    }

}
import java.io.File
import kotlin.io.path.Path

sealed class Command(val name: String, private val description: String, val func: FTPClient.(String) -> Unit) {
    fun register() {
        prCommands[name.substringBefore(' ')] = this
    }

    override fun toString(): String {
        return "$name: $description"
    }

    companion object {
        private val prCommands: MutableMap<String, Command> = mutableMapOf()
        val commands: Map<String, Command>
            get() = prCommands

        fun registerAll() {
            Help.register()
            Dir.register()
            List.register()
            CD.register()
            MkDir.register()
            RmDir.register()
            RmFile.register()
            Load.register()
            Dump.register()
            Quit.register()
        }
    }

    object MkDir: Command("mkdir [name]", "create directory", { directory ->
        mkDir(directory)
    })

    object RmFile: Command("rmf [fileName]", "deletes file", { fileName ->
        rmFile(fileName)
    })

    object RmDir: Command("rmd [directory]", "deletes directory", { directory ->
        rmDir(directory)
    })

    object CD: Command("cd [directory]", "changes root", { directory ->
        goTo(directory)
    })

    object Load : Command("load [filename]", "loads file from server", { fileName ->
        val file = File(fileName)
        println("Loading...")
        loadFile(fileName, file.outputStream())
        println("Done")
    })

    object Dump : Command("dump [pathToFile]", "sends file to server", { path ->
        println("Sending...")
        sendFile(Path(path))
        println("Done")
    })

    object Help : Command("help", "outputs all commands", {
        println("Available commands:")
        commands.forEach { (_, cmd) ->
            println(cmd)
        }
    })

    object Dir : Command("dir", "outputs current directory", {
        println("Current directory: ${getCurrentDirectory()}")
    })

    object List : Command("list", "outputs all files and dirs", {
        print("List:")
        val list = getList().groupBy { it.isDirectory }
        list[true]?.forEach {
            print("\n$it")
        }
        list[false]?.forEach {
            print("\n$it")
        }
        println()
    })

    object Quit : Command("quit", "exit from app", {
        quit()
    })
}

fun FTPClient.loop() {
    Command.registerAll()
    Command.Help.func(this, "")
    while (true) {
        print("> ")
        val line = readLine()?.trim() ?: break
        val cmd = line.substringBefore(' ')
        val args = line.substringAfter(' ', "")
        Command.commands[cmd]?.func?.invoke(this, args) ?: run {
            println("Unknown command")
            Command.Help.func(this, "")
        }
        if (cmd == Command.Quit.name)
            break
    }
}

fun main(args: Array<String>) {
    val host = args[0]
    val port = args[1].toInt()
    val login = args[2]
    val password = args.getOrNull(3)
    val activeAddress = args.getOrNull(4) // ?: "127.0.0.1"
    val debugOutput = false

    FTPClient(host, port, activeAddress, debugOutput).apply {
        login(login, password)
        loop()
    }
}
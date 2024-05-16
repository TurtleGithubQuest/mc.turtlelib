package dev.turtle.turtlelib

import dev.turtle.turtlelib.util.MessageFactory
import dev.turtle.turtlelib.util.configuration.ConfigFactory
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

abstract class TurtlePlugin: JavaPlugin() {
    var configFactory: ConfigFactory = ConfigFactory(this)
    val messageFactory = MessageFactory(this)
    val plugin = this
    val pluginName = plugin.name
    var pluginFolder = pluginName
    var pluginFolderPath = "plugins"
    val pluginVersion: String = this.description.version
    fun getPluginFolder(): File { return File(pluginFolderPath, pluginFolder) }
    fun setPluginFolder(folderName: String, folderPath: String) {
        this.pluginFolder = folderName
        this.pluginFolderPath = folderPath
    }
    abstract fun onStart()
    override fun onEnable() {
        try {
            this.onStart()
            super.onEnable()
        } catch (_: TurtlePluginDisabled) {}
    }
    fun disable(text: String = "&cDisabling $pluginName") {
        val pm = Bukkit.getPluginManager()
        val msg = messageFactory.newMessage(text).enablePrefix()
        msg.send()
        this.isEnabled = false
        pm.getPlugin(pluginName)?.let { pm.disablePlugin(it) }
        throw(TurtlePluginDisabled(msg.text()))
    }
}
class TurtlePluginDisabled(message: String): Exception(message)
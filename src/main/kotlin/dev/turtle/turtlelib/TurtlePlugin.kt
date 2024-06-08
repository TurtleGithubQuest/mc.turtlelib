package dev.turtle.turtlelib

import dev.turtle.turtlelib.gui.TurtleGUI
import dev.turtle.turtlelib.util.CIMutableMap
import dev.turtle.turtlelib.util.MessageFactory
import dev.turtle.turtlelib.util.configuration.ConfigFactory
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

abstract class TurtlePlugin: JavaPlugin() {
    var configFactory: ConfigFactory = ConfigFactory(this)
    val messageFactory = MessageFactory(this)
    var plugin = this
        private set
    var pluginName = this.name
        private set
    var pluginFolder = pluginName
    var pluginFolderPath = "plugins"
    val guis = CIMutableMap<TurtleGUI>()
    val pluginVersion: String = this.description.version
    val eventListeners = mutableListOf<Listener>()
    fun getPluginFolder(): File { return File(pluginFolderPath, pluginFolder) }
    fun setPluginFolder(folderName: String, folderPath: String) {
        this.pluginFolder = folderName
        this.pluginFolderPath = folderPath
    }
    fun reload() {
        eventListeners.clear()
        guis.clear()
        this.onStart()
        HandlerList.unregisterAll(plugin)
        eventListeners.forEach { Bukkit.getPluginManager().registerEvents(it, plugin)}
        if (eventListeners.size > 0)
            messageFactory.newMessage("&7Registered &e${eventListeners.size}&7 event listeners.").enablePrefix().send()
    }
    abstract fun onStart()
    override fun onEnable() {
        try {
            plugin = this
            this.reload()
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
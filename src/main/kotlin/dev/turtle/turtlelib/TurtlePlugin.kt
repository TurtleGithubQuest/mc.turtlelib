package dev.turtle.turtlelib

import dev.turtle.turtlelib.gui.GUIFactory
import dev.turtle.turtlelib.util.ItemFactory
import dev.turtle.turtlelib.util.MessageFactory
import dev.turtle.turtlelib.util.configuration.ConfigFactory
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

abstract class TurtlePlugin: JavaPlugin() {
    lateinit var configFactory: ConfigFactory
    lateinit var messageFactory: MessageFactory
    lateinit var itemFactory: ItemFactory
    lateinit var guiFactory: GUIFactory
    var plugin = this
        private set
    var pluginName = this.name
        private set
    var pluginFolder = pluginName
    var pluginFolderPath = "plugins"
    val pluginVersion: String = this.description.version
    val eventListeners = mutableListOf<Listener>()
    fun getPluginFolder(): File { return File(pluginFolderPath, pluginFolder) }
    fun setPluginFolder(folderName: String, folderPath: String) {
        this.pluginFolder = folderName
        this.pluginFolderPath = folderPath
    }
    fun reload() {
        configFactory = ConfigFactory(this)
        messageFactory = MessageFactory(this)
        itemFactory = ItemFactory(this)
        guiFactory = GUIFactory(this)
        eventListeners.clear()
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
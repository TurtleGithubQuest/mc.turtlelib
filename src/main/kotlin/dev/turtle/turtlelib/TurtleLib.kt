package dev.turtle.turtlelib

import org.bukkit.plugin.java.JavaPlugin

class TurtleLib: TurtlePlugin() {
    companion object {
        lateinit var turtleLib: JavaPlugin private set
        lateinit var libVersion: String private set
    }
    override fun onStart() {
        turtleLib = this
        libVersion = this.description.version
        this.messageFactory
            .setPrefix("&8&l[&2Turtle&9Lib&8&l] ")
            .newMessage("&7v&b$pluginVersion &2enabled&7.").enablePrefix().send()
    }
    override fun onDisable() {
        this.messageFactory.newMessage("&7v&b$pluginVersion &4disabled&7.").enablePrefix().send()
    }
}
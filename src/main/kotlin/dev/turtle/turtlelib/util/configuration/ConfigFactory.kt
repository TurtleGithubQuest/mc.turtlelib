package dev.turtle.turtlelib.util.configuration

import dev.turtle.turtlelib.TurtlePlugin

class ConfigFactory(internal val turtle: TurtlePlugin) {
    internal val registeredConfigs: MutableMap<String, Configuration> = mutableMapOf()
    fun reload() {
        turtle.getPluginFolder().mkdirs()
        for ((_, cfg) in registeredConfigs) {
            cfg.reload()
        }
    }
    fun get(configName: String): Configuration? { return this.registeredConfigs[configName] }
    fun new(configName: String): Configuration { return Configuration(configName, this) }
}
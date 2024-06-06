package dev.turtle.turtlelib.util.configuration

import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.ConfigRenderOptions
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import com.typesafe.config.ConfigFactory as TSConfigFactory
import com.typesafe.config.Config as TSConfig

open class Configuration(val name: String, private val factory: ConfigFactory) {
    init {
        factory.registeredConfigs[name] = this
    }
    open fun onConfigurationLoad() {}
    private var tsConfig: TSConfig = TSConfigFactory.empty()
    private var configFolder: String = "" //todo
    private val m = factory.turtle.messageFactory
    fun getFile(configName: String): File { return File(factory.turtle.getPluginFolder(), "$configName.conf") }
    fun reload() {
        var configFile = getFile(name)
        val pluginFolder = factory.turtle.getPluginFolder()
        File(configFolder).mkdirs()
        if (!configFile.exists() || !configFile.isFile) {
            this::class.java.classLoader.getResourceAsStream("$name.conf")?.let { stream ->
                Files.copy(stream, Paths.get(File(pluginFolder, "$name.conf").path))
            }
            configFile = File(pluginFolder, "$name.conf")
        }
        this.tsConfig = TSConfigFactory.parseFile(configFile)
        val version = this.getStringOrNull("version") ?:"UNKNOWN"
        try {
            this.onConfigurationLoad()
        } catch(ex: ConfigException) {
            when (ex) {
                is Missing -> {
                    ex.printStackTrace()
                    factory.turtle.disable("&7Plugin &cdisabled&7: ${ex.message}.")
                }
                else -> throw(ex)
            }
        }
        m.newMessage("&2Successfully &7loaded &b$name.conf&7 v:&b$version&7.").enablePrefix().send()
    }
    fun hasAllKeys(requiredKeys: Array<String>): Boolean {
        requiredKeys.forEach { key ->
            if (!tsConfig.hasPathOrNull(key)) {
                 m.newMessage("&7Key '&c$key&7' not found in '&c$name&7'.").send()
                return false
            }
        }
        return true
    }
    fun getSection(name: String): TSConfig? {
        try {
            return tsConfig.getConfig(name)
        } catch(ex: ConfigException.Missing) {
            m.newMessage("&7Section '&c$name&7' not found in &e${this@Configuration.name}&7.").enablePrefix().send()
        } catch(ex: ConfigException.WrongType) {
            m.newMessage("&7Section '&c$name&7' is not an object.").enablePrefix().send()
        }
        return null
    }
    fun save() {
        val options = ConfigRenderOptions.concise()
            .setComments(true)
            .setJson(false)
            .setFormatted(true)
        val render = tsConfig.root().render(options)
        Files.write(
            Paths.get(File(factory.turtle.getPluginFolder(), "$configFolder$name.conf").path),
            render.toByteArray()
        )
        this.reload()
    }
    /**
     * Allow safe retrieval of values which may be null
     */
    fun getStringOrNull(path: String): String? {
       return if (tsConfig.hasPathOrNull(path)) {
           if (tsConfig.hasPath(path)) {
               try {
                   tsConfig.getString(path)
               } catch (e: com.typesafe.config.ConfigException.WrongType) {
                    m.newMessage("&7Wrong value type: &c${e.message}.").enablePrefix().send()
                   path
               }
           } else { null }
       } else {
           m.newMessage("&7String '&c$path&7' not found in config.").enablePrefix().send()
           null
       }
    }
}
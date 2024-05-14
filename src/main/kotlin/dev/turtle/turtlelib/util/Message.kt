package dev.turtle.turtlelib.util

import dev.turtle.turtlelib.TurtlePlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

class MessageFactory(private val turtle: TurtlePlugin) {
    val console = Bukkit.getServer().consoleSender
    val placeholderMap = hashMapOf<String, Any?>(
        "PLUGIN_NAME" to turtle.pluginName,
        "PLUGIN_VERSION" to turtle.pluginVersion
    )
    var defaultLanguage = "en_US"
    private var prefix: String? = null
    private var suffix: String? = null
    fun newMessage(text: String): StylizedMessage { return StylizedMessage(text, this) }
    fun setPrefix(value: String): MessageFactory {this.prefix = value; return this }
    fun setSuffix(value: String): MessageFactory {this.suffix = value; return this }
    class StylizedMessage(private var text: String, private val factory: MessageFactory) {
        private var prefixEnabled = false
        private var suffixEnabled = false
        private var configKey = false
        private var placeholders = HashMap<String, Any>()
        private var disablePlaceholders = false
        fun enablePrefix(value: Boolean=true): StylizedMessage { this.prefixEnabled = value; return this }
        fun enableSuffix(value: Boolean=true): StylizedMessage { this.suffixEnabled = value; return this }
        fun fromConfig(value: Boolean=true): StylizedMessage { this.configKey = value; return this}
        fun placeholder(key: String, value: Any): StylizedMessage {this.placeholders[key.uppercase()] = value; return this}
        fun placeholders(hashMap: HashMap<String, Any>): StylizedMessage {this.placeholders.putAll(hashMap); return this}
        fun disablePlaceholders(value: Boolean=true): StylizedMessage {this.disablePlaceholders = value; return this}
        fun text(messageTarget: CommandSender = Bukkit.getServer().consoleSender): String {
            val formattedText = StringBuilder(
                if (configKey) {
                    //TODO: Get target's language
                    factory.turtle.configFactory.get("lang")!!.getSection(factory.defaultLanguage)
                        ?.let { if (it.hasPath(text)) it.getString(text) else text }
                        ?: text
                } else text
            )
            if (prefixEnabled)
                factory.prefix?.let{ formattedText.insert(0, it) }
            if (suffixEnabled)
                factory.suffix?.let{ formattedText.append(it) }
            return ChatColor.translateAlternateColorCodes('&',
                if (disablePlaceholders)
                    formattedText.toString()
                else
                    factory.run { formattedText.toString().parsePlaceholders(placeholders) }
            )
        }
        fun send(messageTarget: CommandSender = Bukkit.getServer().consoleSender) {
            messageTarget.sendMessage(this.text(messageTarget))
        }
    }
    fun String.parsePlaceholders(hashMap: HashMap<String, Any>? = null): String {
        val regex = Regex("%(.*?)%")
        val placeholders = hashMap?.let { placeholderMap + it } ?: placeholderMap
        return regex.replace(this) { matchResult ->
            val placeholder = matchResult.groupValues[1].uppercase()
            if (placeholders.containsKey(placeholder)) {
                placeholders[placeholder]?.toString() ?: matchResult.value
            } else {
                newMessage("&7Invalid placeholder '&c$placeholder&7'.").send()
                matchResult.value
            }
        }
    }
    /**
     * Try to load the language config for the commandSender. If it's not available, we try
     * the default language. If neither are available, the key itself is returned.
     */
    fun CommandSender.getLocalizedMessage(key: String): String {
        val csLang = defaultLanguage //TODO: Get target's language
        val availableLangConfig = turtle.configFactory.get("lang")!!.getSection(csLang)?.let {
            cfgLang ->
                if (cfgLang.hasPath("$csLang.$key"))
                    cfgLang.getConfig(csLang)
                else if (cfgLang.hasPath("$defaultLanguage.$key")) {
                   newMessage("&7Value associated with '&c$key&7' in the lang &7'&e$csLang&7' not found.").send()
                   cfgLang.getConfig(defaultLanguage)
                } else null
        }
        return availableLangConfig
            ?. getString(key)
            ?: run {
                newMessage("&7Value associated with '&c$key&7' in the lang &7'&e$csLang&7' not found.").send()
                key
            }
    }
}
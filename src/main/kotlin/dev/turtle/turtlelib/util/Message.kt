package dev.turtle.turtlelib.util

import dev.turtle.turtlelib.TurtlePlugin
import dev.turtle.turtlelib.util.Font.characterLengthPx
import dev.turtle.turtlelib.util.Font.stringLengthPx
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.ChatColor.COLOR_CHAR
import org.bukkit.command.CommandSender
import java.awt.Color
import java.util.*

class MessageFactory(private val turtle: TurtlePlugin) {
    val console = Bukkit.getServer().consoleSender
    val placeholderMap = hashMapOf<String, Any?>(
        "PLUGIN_NAME" to turtle.pluginName,
        "PLUGIN_VERSION" to turtle.pluginVersion
    )
    var selectedLanguages = hashMapOf<String, String>()
    var defaultLanguage = "en_US"
    var chatWidthPixels = 320
    private var prefix: String? = null
    private var suffix: String? = null
    private var alignTextEnabled = false
    fun newMessage(text: String): StylizedMessage { return StylizedMessage(text, this) }
    fun setPrefix(value: String): MessageFactory {this.prefix = value; return this }
    fun setSuffix(value: String): MessageFactory {this.suffix = value; return this }
    fun setChatWidthPixels(value: Int): MessageFactory {this.chatWidthPixels=value; return this }
    fun setSelectedLanguages(value: HashMap<String, String>): MessageFactory {this.selectedLanguages = value; return this }
    /**
     * (Dis)allow default alignment for messages produced by this factory.
    */
    fun enableAlignment(value: Boolean=true): MessageFactory { this.alignTextEnabled = value; return this }
    class StylizedMessage(private var text: String, private val factory: MessageFactory) {
        private var prefixEnabled = false
        private var suffixEnabled = false
        private var alignTextEnabled = factory.alignTextEnabled
        private var configKey = false
        private var selectedLanguage = factory.defaultLanguage
        private var placeholders = HashMap<String, Any>()
        private var disablePlaceholders = false
        fun enablePrefix(value: Boolean=true): StylizedMessage { this.prefixEnabled = value; return this }
        fun enableSuffix(value: Boolean=true): StylizedMessage { this.suffixEnabled = value; return this }
        fun enableAlignment(value: Boolean=true): StylizedMessage { this.alignTextEnabled = value; return this }
        fun fromConfig(value: Boolean=true): StylizedMessage { this.configKey = value; return this}
        fun placeholder(key: String, value: Any): StylizedMessage {this.placeholders[key.uppercase()] = value; return this}
        fun placeholders(hashMap: HashMap<String, Any>): StylizedMessage {this.placeholders.putAll(hashMap); return this}
        fun disablePlaceholders(value: Boolean=true): StylizedMessage {this.disablePlaceholders = value; return this}
        fun alignMessage(input: String): String {
            val regex = """(LEFT|CENTER|RIGHT)\((.*?)\)|(.*?)(?=(LEFT|CENTER|RIGHT)\((.*?)\)|${'$'})""".toRegex()
            val result = StringBuilder()
            val textList = mutableListOf<MessageSegment>()
            var freeSpace = 200
            var alignments = 0
            regex.findAll(input).forEach { match ->
                val (alignmentStr, content) = match.destructured
                val segment = if(content.isNotBlank()) {
                    val alignedMessage = MessageSegment(
                       Alignment.valueOf(alignmentStr), content
                    )
                    when(alignedMessage.alignment) {
                        Alignment.CENTER, Alignment.RIGHT -> alignments += 1
                        else -> {}
                    }
                    freeSpace -= stringLengthPx(selectedLanguage, alignedMessage.text)
                    alignedMessage
                } else if (match.value.isNotEmpty()) {
                    freeSpace -= stringLengthPx(selectedLanguage, match.value)
                    MessageSegment(Alignment.NONE, match.value)
                } else {
                    null
                }
                segment?.let {
                    textList.add(it)
                }
            }
            val alignmentPxAllocation = freeSpace.floorDiv(alignments.coerceAtLeast(1)).coerceAtLeast(0)
            val spaceText = " ".repeat(alignmentPxAllocation.floorDiv(characterLengthPx(selectedLanguage, ' ')).coerceAtLeast(0))
            val spaceTextCenter = spaceText.drop(spaceText.length.floorDiv(2))
            textList.forEach { segment ->
                result.append(when (segment.alignment) {
                    Alignment.CENTER -> "$spaceTextCenter${segment.text}"
                    Alignment.RIGHT -> "$spaceText${segment.text}"
                    else -> segment.text
                })
            }
            return result.toString()
        }
        /**
        * Applies all color translations on text.
        * - minecraft color code `&color`
        * - hex code `#color#`
        * - hex gradient `#>hex colors and text<#`
        * */
        fun String.bukkolorize(altColorChar: Char='&',startTag: Char='#', endTag: Char='#'): String =
            ChatColor.translateAlternateColorCodes(altColorChar, this)
                .translateHexGradientColorCode(startTag, endTag)
                .translateHexColorCodes(startTag, endTag)
        /**
         * Generates hexadecimal color gradients from color codes defined inside the tags.
         *
         * Example:
         * - Using `#>#000000#Black to white gradient#FFFFFF#<#`
         * In this case, a gradient is generated from black (`#000000`) to white (`#FFFFFF`).
         * */
        fun String.translateHexGradientColorCode(startTag: Char='#', endTag: Char='#'): String {
            val regex = "#>.*?<#".toRegex()
            val gradientBlocks = regex.findAll(this).toList().map { it.value }
            var result = this
            gradientBlocks.forEach { block ->
                val blockContent = block.drop(2).dropLast(2)
                val colorAndTextRegex = """$startTag([0-9a-fA-F]{6})$endTag([^$endTag]*)""".toRegex()
                val matches = colorAndTextRegex.findAll(blockContent).toList()

                val colors = matches.map { Color.decode("#${it.groupValues[1]}") }
                val textSegments = matches.map { it.groupValues[2] }

                var gradientBlock = ""
                for (j in 0 until (colors.size - 1)) {
                        val numChar = textSegments[j].length
                        for (k in 0 until numChar) {
                            val ratio = if (numChar > 1) k.toFloat() / (numChar - 1) else 0f
                            val red = (colors[j].red * (1 - ratio) + colors[j + 1].red * ratio).toInt()
                            val green = (colors[j].green * (1 - ratio) + colors[j + 1].green * ratio).toInt()
                            val blue = (colors[j].blue * (1 - ratio) + colors[j + 1].blue * ratio).toInt()
                            val color = Color(red, green, blue)
                            val hex = startTag + Integer.toHexString(color.rgb).substring(2).uppercase() + endTag

                            gradientBlock += "${hex}${textSegments[j][k]}"
                        }
                    }
                    gradientBlock += textSegments.last()
                    result = result.replace(block, gradientBlock)
            }
            return result
        }
        /**
         * Translates user-friendly hexadecimal color code to Minecraft color code.
         *
         * Example:
         * - `#000000#Black text` generates black-colored string "Black text".
         *
         * This implementation is inspired by the solution shared on SpigotMC forum by `Elementeral`: [Hex color code translate](https://www.spigotmc.org/threads/hex-color-code-translate.449748/#post-3867804)
         */
        fun String.translateHexColorCodes(startTag: Char='#', endTag: Char='#'): String {
            val hexRegex = """$startTag([A-Fa-f0-9]{6})$endTag""".toRegex()
            val result = StringBuffer()
            val temp = hexRegex.replace(this) { matchResult ->
                val colorCode: String = matchResult.groups[1]?.value ?: ""
                val replacement = StringBuilder()
                replacement.append(COLOR_CHAR).append("x")
                for (i in colorCode.indices) {
                    replacement.append(COLOR_CHAR).append(colorCode[i])
                }
                result.append(replacement.toString())
                replacement.toString()
            }
            result.append(temp)

            return result.toString()
        }
        fun text(messageTarget: CommandSender = factory.console): String {
            val formattedText = StringBuilder(
                if (configKey) {
                    selectedLanguage = factory.selectedLanguages[messageTarget.name]?:factory.defaultLanguage
                    val langConfiguration = factory.turtle.configFactory.get("lang")!!
                    val section = langConfiguration.getSection(selectedLanguage)
                        ?: langConfiguration.getSection(factory.defaultLanguage)
                    section?.let { if (it.hasPath(text)) it.getString(text) else text }
                } else text
            )
            if (prefixEnabled)
                factory.prefix?.let{ formattedText.insert(0, it) }
            if (suffixEnabled)
                factory.suffix?.let{ formattedText.append(it) }
            val text =
                (if (disablePlaceholders)
                    formattedText.toString()
                else
                    factory.run { formattedText.toString().parsePlaceholders(placeholders) }).bukkolorize()

            return if (this.alignTextEnabled) {
                alignMessage(text)
            } else text
        }
        fun send(messageTarget: CommandSender = factory.console) {
            messageTarget.sendMessage(this.text(messageTarget))
        }
    }
    fun String.parsePlaceholders(hashMap: HashMap<String, Any>? = null): String {
        var temp = this.replace("\\%", "\u0000")
        val regex = Regex("%(.*?)%")
        val placeholders = hashMap?.let { placeholderMap + it } ?: placeholderMap
        temp = regex.replace(temp) { matchResult ->
            val placeholder = matchResult.groupValues[1].uppercase()
            if (placeholders.containsKey(placeholder)) {
                placeholders[placeholder]?.toString() ?: matchResult.value
            } else {
                newMessage("&7Invalid placeholder '&c$placeholder&7'.").send()
                matchResult.value
            }
        }
        return temp.replace("\u0000", "%")
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
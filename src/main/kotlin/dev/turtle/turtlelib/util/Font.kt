package dev.turtle.turtlelib.util

//todo: Add font-pack loading from configuration file and allow registration of custom font maps.
object Font {
    val en_US = mapOf(
            'I' to 3, 'f' to 4, 'i' to 1, 'j' to 4, 'k' to 4, 'l' to 1, 't' to 4,
            '!' to 1, '@' to 6, '%' to 7, '*' to 3, '(' to 4, ')' to 4, '-' to 5,
            '|' to 1, '[' to 3, ']' to 3, '{' to 4, '}' to 4, ';' to 1, ':' to 1,
            '"' to 3, '\'' to 1, '?' to 5, '/' to 5, '\\' to 5, '.' to 1, ',' to 2, '`' to 2, '~' to 7,
            ' ' to 3, '\n' to 0, '\t' to 2, 'Â' to 0, '§' to 0
    )
    val letterLengthMap = mapOf("en_US" to en_US)
    fun characterLengthPx(language: String, char: Char, isBold: Boolean=false): Int {
        var length = letterLengthMap[language]
            ?.let { it.getOrElse(char) { 5 } }
            ?: run {
                println("TurtleLib error: lettermap not found for language '$language'.")
                5
            }
        if (isBold && char != ' ')
            length += 1
        return length
    }
    fun stringLengthPx(language: String, text: String, isBold: Boolean=false, ignoreColorCodes: Boolean=true): Int {
        val textToSummarize = if (ignoreColorCodes)
            text.replace("#([0-9a-fA-F]{6})#".toRegex(), "")
                .replace("Â?§([0-9a-fA-Fk-oK-OrR])(.*?)(?=Â?§|$)".toRegex(), "$2")
                .replace("§x", "")
            else text
        return textToSummarize.sumOf { characterLengthPx(language, it, isBold) }
    }
}
data class MessageSegment(val alignment: Alignment, val text: String)
enum class Alignment { CENTER, LEFT, RIGHT, NONE }
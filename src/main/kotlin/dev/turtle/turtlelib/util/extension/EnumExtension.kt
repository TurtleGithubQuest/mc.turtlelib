package dev.turtle.turtlelib.util.extension

object EnumExtension {
    inline fun <reified T: Enum<T>> valueOfOrNull(name: String, ignoreCase: Boolean = false): T? =
        enumValues<T>().firstOrNull { it.name.equals(name, ignoreCase = ignoreCase) }
}
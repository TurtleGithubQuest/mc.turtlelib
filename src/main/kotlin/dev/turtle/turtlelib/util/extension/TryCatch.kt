package dev.turtle.turtlelib.util.extension

import dev.turtle.turtlelib.util.MessageFactory

object TryCatch {
    inline fun <T> Any?.tryOrNull(m: MessageFactory? = null, transformer: (Any) -> T): T? {
        return tryOrNullOrEx<T, Exception>( transformer, m)
    }
    inline fun <T, reified E: Throwable> Any?.tryOrNullOrEx(transformer: (Any) -> T, m: MessageFactory? = null): T? {
        return try {
            this?.let(transformer)
        } catch (ex: Exception) {
            if (ex is E)
                m?.newMessage(ex.message.toString())?.enablePrefix()?.send()
            else throw ex
            null
        }
    }
}
package dev.turtle.turtlelib.util.configuration

import com.typesafe.config.ConfigException
import dev.turtle.turtlelib.util.MessageFactory
import dev.turtle.turtlelib.util.configuration.ConfigResult.Ok
import dev.turtle.turtlelib.util.configuration.ConfigResult.Err
import com.typesafe.config.Config as TSConfig

object ConfigUtils {
    fun TSConfig.getStringOrNull(path: String): ConfigResult<String?> {
        return if (this.hasPathOrNull(path)) {
               if (this.hasPath(path)) {
                   try {
                       Ok(this.getString(path))
                   } catch (e: com.typesafe.config.ConfigException.WrongType) {
                       Err(path, ConfigError.WRONG_TYPE, "&7Wrong value type: &c${e.message}.")
                   }
               } else { Err(null, ConfigError.PATH_NOT_FOUND, "&7Path not found") }
           } else {
               Err(null, ConfigError.PATH_NOT_FOUND, "&7String '&c$path&7' not found in config.")
           }
    }
    fun TSConfig.getBooleanOrNull(path: String): ConfigResult<Boolean?> {
        return this.getStringOrNull(path).let {
            when(it) {
                is Ok -> Ok(it.value.toBoolean())
                is Err -> Err(null, it.error, it.message)
            }
        }
    }
    fun TSConfig.getIntOrNull(path: String): ConfigResult<Int?> {
        this.getStringOrNull(path).let {
            val r = when (it) {
                is Ok ->
                    try { return Ok(it.value?.toInt())
                    } catch (ex: NumberFormatException) { Pair(ConfigError.WRONG_TYPE, ex.message) }
                is Err -> Pair(it.error, it.message)
            }
            return Err(null, r.first, r.second?:"")
        }
    }
    @Suppress("UNCHECKED_CAST")
    fun <V>TSConfig.getListOrNull(path: String): ConfigResult<List<V>?> {
        return try {
            Ok(this.getStringList(path).map { it as V })
        } catch (ex: ConfigException) {
            Err(null, ConfigError.PATH_NOT_FOUND, ex.message?:"$path not found")
        } catch (ex: TypeCastException) {
            Err(null, ConfigError.WRONG_TYPE, ex.message?:"Invalid value in list. $path")
        }
    }
}
sealed class ConfigResult<V> {
    data class Ok<V>(val value: V): ConfigResult<V>()
    data class Err<V>(val value: V, val error: ConfigError, val message: String): ConfigResult<V>()
    fun isOk(): Boolean = this is Ok<V>
    fun getValue(messageFactory: MessageFactory?=null): V = when (this) {
        is Ok -> this.value
        is Err -> {
            messageFactory?.newMessage(this.message)?.enablePrefix()?.send()
            this.value
        }
    }
}
enum class ConfigError {
    WRONG_TYPE,
    PATH_NOT_FOUND
}
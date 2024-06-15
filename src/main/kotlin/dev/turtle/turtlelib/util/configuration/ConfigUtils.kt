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
    fun <T> TSConfig.getAndTransform(path: String, transformer: (String?) -> T): ConfigResult<T?> {
        this.getStringOrNull(path).let {
            return when (it) {
                is Ok ->
                    try { Ok(transformer(it.value)) }
                    catch (ex: NumberFormatException) { Err(null, ConfigError.WRONG_TYPE, ex.message.toString()) }
                    catch (ex: IllegalArgumentException) { Err(null, ConfigError.WRONG_TYPE, ex.message.toString()) }
                is Err -> Err(null, it.error, it.message)
            }
        }
    }
    fun TSConfig.getDoubleOrNull(path: String): ConfigResult<Double?> {
        return getAndTransform(path) { it?.toDouble() }
    }
    fun TSConfig.getIntOrNull(path: String): ConfigResult<Int?> {
        return getAndTransform(path) { it?.toInt() }
    }
    fun TSConfig.getBooleanOrNull(path: String): ConfigResult<Boolean?> {
        return getAndTransform(path) { it?.toBoolean() }
    }
    @Suppress("UNCHECKED_CAST")
    fun <V>TSConfig.getListOrNull(path: String): ConfigResult<List<V>?> {
        return try {
            Ok(this.getAnyRefList(path).map { it as V })
        } catch (ex: ConfigException) {
            Err(null, ConfigError.PATH_NOT_FOUND, ex.message?:"$path not found")
        } catch (ex: TypeCastException) {
            Err(null, ConfigError.WRONG_TYPE, ex.message?:"Invalid value in list. $path")
        } catch(ex: ClassCastException) {
            Err(null, ConfigError.WRONG_TYPE, ex.message?:"Cast failed. $path")
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
package dev.turtle.turtlelib

import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import kotlin.reflect.KClass
import dev.turtle.turtlelib.util.MessageFactory
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

abstract class TurtleCommand(val commandName: String, val turtle: TurtlePlugin) : TabExecutor {
    val subCommands: MutableMap<String, TurtleSubCommand> = mutableMapOf()
    private val subCommandsList by lazy { subCommands.keys.toList() }
    fun newSubCommand(turtleSubCommand: TurtleSubCommand) {
        this.subCommands[turtleSubCommand.subCommandName] = turtleSubCommand
    }

    override fun onTabComplete(
        cs: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): List<String>? {
        return args.getOrNull(0)?.let {
            subCommands[it]?.let { subCmd ->
                if (cs.hasPermission("turtle.${this.commandName}.${subCmd.subCommandName}")) {
                    subCmd.argumentData.values.toList().getOrNull(args.size - 2)?.let { arg ->
                        subCmd.updateValues(cs, cmd, label, args)
                        subCmd.onSuggestion(arg)
                        arg.suggestions
                    }
                } else null
            }
        } ?: run {
            if (args.size == 1)
                subCommands.filter { (_, subCmd) ->
                    cs.hasPermission("turtle.${this.commandName}.${subCmd.subCommandName}")
                }.keys.toList()
            else null
        }
    }
}
abstract class TurtleSubCommand(val subCommandName: String, turtleCommand: TurtleCommand) {
    protected val commandName = turtleCommand.commandName
    protected val turtle = turtleCommand.turtle
    val argumentData: MutableMap<String, ArgumentData> = mutableMapOf()
    var cs: CommandSender? = null
    var args: Array<out String> = arrayOf()
    var label: String? = null
    val argumentUsage: String by lazy {
        argumentData.values.joinToString(" ") {
            it.text
        }
    }

    init {
        turtleCommand.newSubCommand(this)
    }

    fun updateValues(cs: CommandSender, cmd: Command, label: String, args: Array<out String>) {
        this.cs = cs
        this.args = args
        this.label = label
    }

    fun execute(cs: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        val requiredPermissions = "turtle.${this.commandName}.${this.subCommandName}"
        return if (cs.hasPermission(requiredPermissions)) {
            this.updateValues(cs, cmd, label, args)
            this.onCommand()
        } else {
            turtle.messageFactory.newMessage("command.no-permissions").fromConfig().send(cs)
            turtle.messageFactory.newMessage("&7Player '&e${cs.name}&7' doesn't have enough permissions: '&c$requiredPermissions&7'.")
                .enablePrefix().send()
            true
        }
    }

    abstract fun onCommand(): Boolean
    abstract fun onSuggestion(argumentData: ArgumentData)
    fun getValue(index: String, defaultValue: Any? = null, notifyErrors: Boolean = true): Any? {
        return argumentData[index]?.let { ad ->
            ad.getValue(defaultValue, notifyErrors)?.let {
                when (it) {
                    is Result -> return null
                    else -> it
                }
            } ?: run {
                if (notifyErrors)
                    turtle.messageFactory.newMessage("command.usage").enablePrefix(false).placeholders(
                        hashMapOf(
                            "ARGUMENTS" to argumentUsage,
                            "SUBCOMMAND" to (args.getOrNull(0) ?: subCommandName),
                            "COMMAND" to (label ?: commandName)
                        )
                    ).fromConfig().send(cs!!)
                null
            }
        }
    }

    fun CommandSender.sendLocalizedMessage(path: String) {
        turtle.messageFactory.newMessage(path).fromConfig().send(this)
    }

    inner class ArgumentData(
        val argumentName: String,
        suggestionsList: List<Any>?,
        val argumentType: KClass<*>,
        val defaultValue: Any? = null,
        val isRequired: Boolean = true,
    ) {
        var suggestions: List<String>? = null
        fun updateSuggestions(list: List<Any>?) {
            this.suggestions = list?.map {
                when (it) {
                    is Player -> it.name
                    else -> it.toString()
                }
            }
        }

        init {
            updateSuggestions(suggestionsList); argumentData[argumentName] = this
        }

        val configPath = "command.$commandName.$subCommandName"
        val text: String by lazy {
            if (isRequired) "<$argumentName>"
            else "[$argumentName]"
        }

        /// + command & subcommand
        val index by lazy { argumentData.values.indexOf(this) + 1 }
        fun getValue(defaultValue: Any? = null, notifyErrors: Boolean = true): Any? {
            val default = this.defaultValue ?: defaultValue
            var errorMessagePath: String? = null
            var argValue: Any?
            return args.getOrNull(index).let {
                argValue = it ?: default
                argValue?.toString()?.let { v ->
                    when (argumentType) {
                        Player::class -> Bukkit.getPlayer(v) ?: run {
                            errorMessagePath = "command.player-not-found"; null
                        }

                        String::class -> v.ifBlank { errorMessagePath = "$configPath.invalid-type"; null }
                        Int::class -> v.toIntOrNull() ?: run {
                            errorMessagePath = "$configPath.invalid-type"; null
                        }

                        else -> null
                    }
                }
            } ?: run {
                val errorMessage = turtle.messageFactory
                    .newMessage(errorMessagePath ?: run {
                        if (isRequired) "$configPath.is-null"
                        else return Result.OK
                    })
                    .placeholder("argument", argumentName)
                    .placeholder("argvalue", argValue.toString())
                    .placeholder(argumentName, argValue.toString())
                    .placeholder("type", argumentType.simpleName.toString())
                    .fromConfig()
                if (notifyErrors)
                    errorMessage.send(cs!!)
                return Result.Error(errorMessage)
            }
        }
    }
}
sealed class Result {
    data object OK : Result()
    data class Error(val text: MessageFactory.StylizedMessage) : Result()
}
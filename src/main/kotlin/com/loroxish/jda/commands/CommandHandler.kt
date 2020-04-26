package com.loroxish.jda.commands

import com.google.inject.Injector
import com.google.inject.ProvisionException
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass

internal class CommandHandler(
    commandMapping: CommandMapping,
    private val injector: Injector) {

    private val commandMap = commandMapping.commandMap
    private val remainderCommandMap = commandMapping.remainderCommandMap
    private val argumentParsers = commandMapping.argumentParsers

    private companion object {
        private const val COMMAND_PREFIX = "!"
    }

    fun handleCommand(event: MessageReceivedEvent) {
        if (event.author.isBot) {
            return
        }

        val messageText = event.message.contentDisplay
        if (!messageText.startsWith(COMMAND_PREFIX) || messageText.replace(COMMAND_PREFIX, "").isEmpty()) {
            return
        }

        val inputString = messageText.replaceFirst(COMMAND_PREFIX, "")
        when (val command = findCommand(inputString)) {
            is CommandExecution.Error -> return event.channel.sendMessage("Debug: ${command.message}").queue()
            is CommandExecution.SelectedCommand -> {
                val definition = createDefinitionInstance(command.signature.clazz, event)
                    ?: return event.channel
                        .sendMessage("Failed to create instance ${command.signature.clazz.simpleName}").queue()

                try {
                    command.signature.function.call(definition, *command.parameters)
                } catch (e: InvocationTargetException) {
                    e.targetException?.run { message?.let { event.channel.sendMessage(it).queue() } }
                }
            }
        }
    }

    private fun createDefinitionInstance(
        clazz: KClass<out BaseCommandDefinition>,
        event: MessageReceivedEvent
    ): BaseCommandDefinition? {

        try {
            val definition = injector.getInstance(clazz.java) ?: return null

            definition.setContext(
                CommandContext(
                    event.jda,
                    if (event.isFromGuild) event.guild else null,
                    event.channel,
                    event.author,
                    event.message))

            return definition
        } catch (ex: ProvisionException) {
            return null
        }
    }

    private fun findCommand(inputString: String): CommandExecution {

        val commandParts = inputString.split("""\s+""".toRegex())

        var error = CommandExecution.Error("Something went wrong")

        commandParts.indices
            .map { commandParts.subList(0, it + 1).joinToString(" ") to commandParts.drop(it + 1) }
            .forEach{ (commandString, parameterInputs) ->
                var command = parseCommandMap(commandString, parameterInputs)
                if (command is CommandExecution.Error) {
                    error = command
                    command = parseRemainderCommandMap(commandString, parameterInputs)
                }
                if (command is CommandExecution.SelectedCommand) {
                    return command
                }
            }
        return error
    }

    private fun parseCommandMap(commandString: String, parameterInputs: List<String>): CommandExecution {

        val matchingCommands = commandMap[commandString] ?: return CommandExecution.Error("Command not found")

        val candidates = matchingCommands[parameterInputs.size]

        if (candidates.isNullOrEmpty()) {
            return CommandExecution.Error("Incorrect number of parameters")
        }

        outer@ for (candidate in candidates) {
            val parameterTypes = candidate.commandInfo.parameters.map { it.type }
            val parameters = mutableListOf<Any>()

            for (i in parameterTypes.indices) {
                val parameterValue =
                    argumentParsers[parameterTypes[i]]?.parseArgument(parameterInputs[i]) ?: continue@outer
                parameters.add(parameterValue)
            }
            return CommandExecution.SelectedCommand(candidate, parameters.toTypedArray())
        }
        return CommandExecution.Error("No command with matching parameter types")
    }

    private fun parseRemainderCommandMap(commandString: String, parameterInputs: List<String>): CommandExecution {

        val matchingCommands = remainderCommandMap[commandString] ?: return CommandExecution.Error("Command not found")

        for (size in parameterInputs.size downTo 1) {

            val candidates = matchingCommands[size]

            if (candidates.isNullOrEmpty()) {
                continue
            }

            outer@ for (candidate in candidates) {
                val parameterTypes = candidate.commandInfo.parameters.map { it.type }
                val parameters = mutableListOf<Any>()

                for (i in parameterTypes.indices) {
                    val parameterToParse = if (i + 1 >= parameterTypes.size) {
                        parameterInputs.drop(i).joinToString(" ")
                    } else {
                        parameterInputs[i]
                    }

                    val parameterValue = argumentParsers[parameterTypes[i]]?.parseArgument(parameterToParse) ?: continue@outer

                    parameters.add(parameterValue)

                    if (i + 1 >= parameterTypes.size) {
                        break
                    }
                }
                return CommandExecution.SelectedCommand(candidate, parameters.toTypedArray())
            }
        }
       return CommandExecution.Error("Failed to parse remainder")
    }

    sealed class CommandExecution {
        data class SelectedCommand(val signature: InternalCommandInfo, val parameters: Array<*>) : CommandExecution()
        data class Error(val message: String) : CommandExecution()
    }
}
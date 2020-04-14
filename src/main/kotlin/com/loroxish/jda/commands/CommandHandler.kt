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

        val commandParts = messageText.replaceFirst(COMMAND_PREFIX, "").split("""\s+""".toRegex())
        var command = findCommand(commandParts)
        if (command is CommandExecution.Error && remainderCommandMap.isNotEmpty()) {
            val remainderCommand = findRemainderCommand(commandParts)
            if (remainderCommand is CommandExecution.Success) {
                command = remainderCommand
            }
        }
        when (command) {
            is CommandExecution.Error -> return event.channel.sendMessage(command.message).queue()
            is CommandExecution.Success -> {
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

    private fun findCommand(commandParts: List<String>): CommandExecution {

        val matchingCommands = commandMap[commandParts[0]] ?: return CommandExecution.Error("Command not found")

        val parameterInputs = commandParts.drop(1)

        val candidates = matchingCommands[parameterInputs.size]

        if (candidates == null || candidates.isEmpty()) {
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
            return CommandExecution.Success(candidate, parameters.toTypedArray())
        }
        return CommandExecution.Error("No command with matching parameter types")
    }

    private fun findRemainderCommand(commandParts: List<String>): CommandExecution {

        val matchingCommands = remainderCommandMap[commandParts[0]] ?: return CommandExecution.Error("No remainder command")

        val parameterInputs = commandParts.drop(1)

        for (size in parameterInputs.size downTo 1) {

            val candidates = matchingCommands[size]

            if (candidates == null || candidates.isEmpty()) {
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
                return CommandExecution.Success(candidate, parameters.toTypedArray())
            }
        }
        return CommandExecution.Error("Failed to parse remainder")
    }

    sealed class CommandExecution {
        data class Success(val signature: InternalCommandInfo, val parameters: Array<*>) : CommandExecution()
        data class Error(val message: String) : CommandExecution()
    }
}
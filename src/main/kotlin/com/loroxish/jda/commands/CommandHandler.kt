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
            is SelectedCommand.Error -> return event.channel.sendMessage(command.message).queue()
            is SelectedCommand.Success -> {

                val context =
                    CommandContext(
                        event.jda,
                        if (event.isFromGuild) event.guild else null,
                        event.channel,
                        event.author,
                        event.message)

                checkPreconditions(command.signature.commandInfo, context)?.let {
                    return event.channel.sendMessage(it.message).queue()
                }

                val definition = createDefinitionInstance(command.signature.clazz, context)
                    ?: return event.channel
                        .sendMessage("Failed to create instance ${command.signature.clazz.simpleName}").queue()

                try {
                    command.signature.function.call(definition, *command.parameters.toTypedArray())
                } catch (e: InvocationTargetException) {
                    e.targetException?.run { message?.let { event.channel.sendMessage(it).queue() } }
                }
            }
        }
    }

    private fun checkPreconditions(
        commandInfo: CommandInfo,
        context: CommandContext
    ): EvaluationResult.Failure? {

        return commandInfo.preconditions
            .map { it.checkCanExecute(context, commandInfo) }
            .filterIsInstance<EvaluationResult.Failure>()
            .firstOrNull()
    }

    private fun createDefinitionInstance(
        clazz: KClass<out BaseCommandDefinition>,
        context: CommandContext
    ): BaseCommandDefinition? {

        try {
            val definition = injector.getInstance(clazz.java) ?: return null
            definition.setContext(context)
            return definition
        } catch (ex: ProvisionException) {
            return null
        }
    }

    private fun findCommand(inputString: String): SelectedCommand {

        val commandParts = inputString.split("""\s+""".toRegex())

        var error = SelectedCommand.Error("Something went wrong")

        commandParts.indices
            .map { commandParts.subList(0, it + 1).joinToString(" ") to commandParts.drop(it + 1) }
            .forEach{ (commandString, parameterInputs) ->
                var command = parseCommandMap(commandString, parameterInputs)
                if (command is SelectedCommand.Error) {
                    error = command
                    command = parseRemainderCommandMap(commandString, parameterInputs)
                }
                if (command is SelectedCommand.Success) {
                    return command
                }
            }
        return error
    }

    private fun parseCommandMap(commandString: String, parameterInputs: List<String>): SelectedCommand {

        val matchingCommands = commandMap[commandString] ?: return SelectedCommand.Error("Command not found")

        val candidates = matchingCommands[parameterInputs.size]

        if (candidates.isNullOrEmpty()) {
            return SelectedCommand.Error("Incorrect number of parameters")
        }

        outer@ for (candidate in candidates) {
            val parameterTypes = candidate.commandInfo.parameters.map { it.type }
            val parameters = mutableListOf<Any>()

            for (i in parameterTypes.indices) {
                val parameterValue =
                    argumentParsers[parameterTypes[i]]?.parseArgument(parameterInputs[i]) ?: continue@outer
                parameters.add(parameterValue)
            }
            return SelectedCommand.Success(candidate, parameters)
        }
        return SelectedCommand.Error("No command with matching parameter types")
    }

    private fun parseRemainderCommandMap(commandString: String, parameterInputs: List<String>): SelectedCommand {

        val matchingCommands = remainderCommandMap[commandString] ?: return SelectedCommand.Error("Command not found")

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
                return SelectedCommand.Success(candidate, parameters)
            }
        }
       return SelectedCommand.Error("Failed to parse remainder")
    }

    private sealed class SelectedCommand {
        data class Success(val signature: InternalCommandInfo, val parameters: List<*>) : SelectedCommand()
        data class Error(val message: String) : SelectedCommand()
    }
}
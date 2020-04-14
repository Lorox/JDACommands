package com.loroxish.jda.commands

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provides
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter


class CommandService (private val jda: JDA, injector: Injector = Guice.createInjector()) {

    private val handler: CommandHandler

    private val listener: CommandListener

    init {
        val commandMapping =
            CommandLoader.loadCommandMapping(
                argumentParsers = listOf(
                    IntParser(),
                    DoubleParser(),
                    UserParser(jda),
                    RoleParser(jda),
                    TextChannelParser(jda),
                    BooleanParser(),
                    StringParser()))

        handler = CommandHandler(
            commandMapping,
            injector.createChildInjector(
                CommandServiceModule(
                    commandMapping.commandMap
                        .flatMap { x -> x.value.flatMap { y -> y.value.map { it.commandInfo } } }
                        .union(commandMapping.remainderCommandMap.flatMap {
                                x -> x.value.flatMap { y -> y.value.map { it.commandInfo } } })
                        .toSet()))!!)

        listener = CommandListener(handler)
    }

    fun attachCommandListener() {
        jda.addEventListener(listener)
    }

    fun removeCommandListener() {
        jda.addEventListener(listener)
    }

    private class CommandListener(private val handler: CommandHandler) : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            super.onMessageReceived(event)
            handler.handleCommand(event)
        }
    }

    private class CommandServiceModule(private val commands: Set<CommandInfo>): AbstractModule() {

        @Provides
        @Commands
        fun commands(): Set<CommandInfo> {
            return commands
        }
    }
}
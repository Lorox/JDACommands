package com.loroxish.jda.commands

import net.dv8tion.jda.api.entities.Message

abstract class BaseCommandDefinition {

    protected lateinit var context: CommandContext private set

    internal fun setContext(context: CommandContext) {
        this.context = context
    }

    protected fun reply(message: Message) {
        if (!::context.isInitialized) {
            return
        }
        context.channel.sendMessage(message).queue()
    }

    protected fun reply(message: String) {
        if (!::context.isInitialized) {
            return
        }
        context.channel.sendMessage(message).queue()
    }
}
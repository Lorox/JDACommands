package com.loroxish.jda.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import kotlin.reflect.KType

data class CommandInfo(
    val name: String,
    val parameters: List<ParameterInfo>,
    val summary: String? = null,
    val remarks: String? = null,
    val hasRemainder: Boolean = false)

data class ParameterInfo(
    val name: String,
    val type: KType,
    val summary: String? = null)

data class CommandContext(
    val jda: JDA,
    val guild: Guild?,
    val channel: MessageChannel,
    val user: User,
    val message: Message)

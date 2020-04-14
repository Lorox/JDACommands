package com.loroxish.jda.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import kotlin.reflect.KType
import kotlin.reflect.full.createType

internal interface ArgumentParser<T> {
    val type: KType
    fun parseArgument(arg: String): T?
}

internal class IntParser : ArgumentParser<Int> {
    override val type: KType = Int::class.createType()

    override fun parseArgument(arg: String): Int? = arg.toIntOrNull()
}

internal class DoubleParser : ArgumentParser<Double> {
    override val type: KType = Double::class.createType()

    override fun parseArgument(arg: String): Double? = arg.toDoubleOrNull()
}

internal class UserParser(val jda: JDA) : ArgumentParser<User> {
    override val type: KType = User::class.createType()

    override fun parseArgument(arg: String): User? {
        val id =
            userPattern.matchEntire(arg)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
        return jda.getUserById(id)
    }

    private companion object {
        private val userPattern = """<@!?(\d+)>""".toRegex()
    }
}

internal class RoleParser(val jda: JDA) : ArgumentParser<Role> {
    override val type: KType = Role::class.createType()

    override fun parseArgument(arg: String): Role? {
        val id =
            rolePattern.matchEntire(arg)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
        return jda.getRoleById(id)
    }

    private companion object {
        private val rolePattern = """<@&(\d+)>""".toRegex()
    }
}

internal class TextChannelParser(val jda: JDA) :
    ArgumentParser<TextChannel> {
    override val type: KType = TextChannel::class.createType()

    override fun parseArgument(arg: String): TextChannel? {
        val id =
            textChannelPattern.matchEntire(arg)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
        return jda.getTextChannelById(id)
    }

    private companion object {
        private val textChannelPattern = """<@#(\d+)>""".toRegex()
    }
}

internal class BooleanParser : ArgumentParser<Boolean> {
    override val type: KType = Boolean::class.createType()

    override fun parseArgument(arg: String): Boolean? =
        when (arg.toLowerCase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }
}

internal class StringParser : ArgumentParser<String> {
    override val type: KType = String::class.createType()

    override fun parseArgument(arg: String): String = arg
}


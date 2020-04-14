package com.loroxish.jda.commands

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType

internal data class CommandMapping(
    val commandMap: Map<String, Map<Int, List<InternalCommandInfo>>>,
    val remainderCommandMap: Map<String, Map<Int, List<InternalCommandInfo>>>,
    val argumentParsers: Map<KType, ArgumentParser<*>>)

internal data class InternalCommandInfo(
    val commandInfo: CommandInfo,
    val clazz: KClass<out BaseCommandDefinition>,
    val function: KFunction<*>)

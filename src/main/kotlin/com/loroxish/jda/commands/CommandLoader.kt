package com.loroxish.jda.commands

import com.google.inject.Inject
import org.reflections.Reflections
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

internal object CommandLoader {

    fun loadCommandMapping(
        reflections: Reflections = Reflections(""),
        argumentParsers: List<ArgumentParser<*>>
    ): CommandMapping {

        val parserTypes = argumentParsers.map { it.type }

        val commandInfoPairs = reflections.getSubTypesOf(BaseCommandDefinition::class.java)
            .mapNotNull { it.kotlin }
            .filter {
                it.constructors.any {
                        constructor -> constructor.parameters.isEmpty()
                        || constructor.findAnnotation<Inject>() != null } }
            .flatMap { buildCommandInfoPairs(it, parserTypes) }

        return CommandMapping(
            getCommandMap(commandInfoPairs, parserTypes, false),
            getCommandMap(commandInfoPairs, parserTypes, true),
            argumentParsers.map { it.type to it }.toMap())
    }

    private fun buildCommandInfoPairs(definition: KClass<out BaseCommandDefinition>, parserTypes: List<KType>):
            List<Pair<String, InternalCommandInfo>> =
        definition.memberFunctions
            .filter { func -> func.parameters.drop(1).all { parserTypes.contains(it.type) } }
            .mapNotNull { func ->
                func.findAnnotation<Command>()
                    ?.let {
                        val commandInfo =
                            CommandInfo(
                                it.name,
                                buildParameterInfo(func),
                                func.findAnnotation<Summary>()?.summary,
                                func.findAnnotation<Remarks>()?.remarks,
                                checkFunctionHasRemainder(func.parameters))
                        it.name to InternalCommandInfo(commandInfo, definition, func)
                    }
            }

    private fun buildParameterInfo(func: KFunction<*>): List<ParameterInfo> =
        func.parameters.drop(1)
            .map {
                ParameterInfo(
                    it.name ?: "unknown",
                    it.type,
                    it.findAnnotation<Summary>()?.summary)
            }

    private fun checkFunctionHasRemainder(parameters: List<KParameter>): Boolean {
        val last = parameters.lastOrNull() ?: return false
        return last.type == String::class.createType() && last.findAnnotation<Remainder>() != null
    }

    private fun getCommandMap(
        commandInfoPairs: List<Pair<String, InternalCommandInfo>>,
        parserTypes: List<KType>,
        filterForRemainders: Boolean
    ): Map<String, Map<Int, List<InternalCommandInfo>>> =
        commandInfoPairs
            .filter { (_, info) -> filterForRemainders == info.commandInfo.hasRemainder }
            .groupBy(
                keySelector = { it.first },
                valueTransform = {
                    it.second.commandInfo.parameters.size to it.second
                })
            .mapValues { entry ->
                entry.value.groupBy(keySelector = { it.first }, valueTransform = { it.second })
                    .mapValues { it.value.sortedWith(CommandInfoComparator(parserTypes)) }
            }

    class CommandInfoComparator(private val types: List<KType>) : Comparator<InternalCommandInfo> {

        override fun compare(first: InternalCommandInfo, second: InternalCommandInfo): Int =
            first.commandInfo.parameters
                .zip(second.commandInfo.parameters)
                .map { (a, b) -> a.type to b.type }
                .map { (a, b) -> types.indexOf(a) to types.indexOf(b) }
                .firstOrNull { (a, b) -> a != b }
                ?.let { it.first - it.second } ?: 0
    }
}
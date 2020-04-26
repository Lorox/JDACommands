package com.loroxish.jda.commands

import com.google.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.reflections.Reflections
import kotlin.reflect.full.createType
import kotlin.reflect.full.functions

@RunWith(MockitoJUnitRunner::class)
internal class CommandLoaderTest {

    private companion object {
        private val stringType = String::class.createType()
        private val intType = Int::class.createType()
    }

    @Mock
    private lateinit var reflections: Reflections

    @Mock
    private lateinit var mockIntParser: ArgumentParser<Int>

    @Mock
    private lateinit var mockStringParser: ArgumentParser<String>

    // This test is horribly monolithic, but I really cannot be bothered to break it down
    @Test
    fun loadCommandMapping() {
        // Given
        `when`(reflections.getSubTypesOf(BaseCommandDefinition::class.java))
            .thenReturn(
                setOf(
                    TestDefinition::class.java,
                    OtherTestDefinition::class.java,
                    TestDefinitionWithPrefix::class.java,
                    FilteredTestDefinition::class.java))

        `when`(mockIntParser.type)
            .thenReturn(intType)

        `when`(mockStringParser.type)
            .thenReturn(stringType)

        val expected =
            CommandMapping(
                mapOf("test" to
                        mapOf(0 to listOf(
                            InternalCommandInfo(
                                CommandInfo(
                                    "test",
                                    listOf(),
                                    "A summary of the base command",
                                    "Remarks on the base command"),
                                TestDefinition::class,
                                TestDefinition::class.functions.first {
                                    it.name == "testCommand" && it.parameters.size == 1 })),
                            1 to listOf(
                                InternalCommandInfo(
                                    CommandInfo(
                                        "test",
                                        listOf(ParameterInfo("int", intType))),
                                    TestDefinition::class,
                                    TestDefinition::class.functions.first{
                                        it.parameters.map { p -> p.type}.drop(1) == listOf(intType) }),
                                InternalCommandInfo(
                                    CommandInfo(
                                        "test",
                                        listOf(ParameterInfo("string", stringType))),
                                    TestDefinition::class,
                                    TestDefinition::class.functions.first{
                                        it.parameters.map { p -> p.type}.drop(1) == listOf(stringType) })),
                            2 to listOf(
                                InternalCommandInfo(
                                    CommandInfo(
                                        "test",
                                        listOf(
                                            ParameterInfo("int1", intType),
                                            ParameterInfo("int2", intType))),
                                    TestDefinition::class,
                                    TestDefinition::class.functions.first{
                                        it.parameters.map { p -> p.type}.drop(1) == listOf(intType, intType) }),
                                InternalCommandInfo(
                                    CommandInfo(
                                        "test",
                                        listOf(
                                            ParameterInfo("int", intType),
                                            ParameterInfo("string", stringType))),
                                    TestDefinition::class,
                                    TestDefinition::class.functions.first{
                                        it.parameters.map { p -> p.type}.drop(1) == listOf(intType, stringType) }),
                                InternalCommandInfo(
                                    CommandInfo(
                                        "test",
                                        listOf(
                                            ParameterInfo("string1", stringType),
                                            ParameterInfo("string2", stringType))),
                                    OtherTestDefinition::class,
                                    OtherTestDefinition::class.functions.first{
                                        it.parameters.map { p -> p.type}.drop(1) == listOf(stringType, stringType) }))),
                    "otherCommand" to
                            mapOf(0 to listOf(
                                InternalCommandInfo(
                                    CommandInfo("otherCommand", listOf()),
                                    TestDefinition::class,
                                    TestDefinition::class.functions.first {
                                        it.name == "otherCommand" && it.parameters.size == 1 }))),
                    "prefix test" to
                            mapOf(0 to listOf(
                                InternalCommandInfo(
                                    CommandInfo("prefix test", listOf()),
                                    TestDefinitionWithPrefix::class,
                                    TestDefinitionWithPrefix::class.functions.first { it.name == "testCommand" })))),
                mapOf("test" to
                        mapOf(1 to listOf(
                            InternalCommandInfo(
                                CommandInfo(
                                    "test",
                                    listOf(ParameterInfo("string", stringType)),
                                    hasRemainder = true),
                                TestDefinition::class,
                                TestDefinition::class.functions.first { it.name == "testCommandWithRemainder" })))),
                mapOf(
                    intType to mockIntParser,
                    stringType to mockStringParser))

        // When
        val result = CommandLoader.loadCommandMapping(reflections, listOf(mockIntParser, mockStringParser))

        // Then
        assertEquals(expected, result)
    }

    private class TestDefinition : BaseCommandDefinition() {

        @Command("test")
        @Summary("A summary of the base command")
        @Remarks("Remarks on the base command")
        fun testCommand() { }

        @Command("test")
        fun testCommand(int: Int, string: String) { }

        @Command("test")
        fun testCommand(int: Int) { }

        @Command("test")
        fun testCommand(int1: Int, @Remainder int2: Int) { }

        @Command("test")
        fun testCommand(string: String) { }

        @Command("test")
        fun shouldBeFiltered(double: Double) { }

        @Command("test")
        fun testCommandWithRemainder(@Remainder string: String) { }

        @Command("otherCommand")
        fun otherCommand() { }
    }

    private class OtherTestDefinition @Inject constructor(injectedThing: Int) : BaseCommandDefinition() {

        @Command("test")
        fun testCommand(@Remainder string1: String, string2: String) { }
    }

    @Prefix("prefix")
    private class TestDefinitionWithPrefix : BaseCommandDefinition() {

        @Command("test")
        fun testCommand() { }
    }

    private class FilteredTestDefinition(injectedThing: Int) : BaseCommandDefinition() {

        @Command("test")
        fun testCommand(int1: Int, int2: Int, int3: Int) { }
    }
}
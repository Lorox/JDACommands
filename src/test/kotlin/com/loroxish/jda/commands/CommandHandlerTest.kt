package com.loroxish.jda.commands

import com.google.inject.Injector
import com.google.inject.ProvisionException
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType

@RunWith(MockitoJUnitRunner::class)
internal class CommandHandlerTest {

    private companion object {
        private val stringType = String::class.createType()
        private val intType = Int::class.createType()
    }

    @Mock
    private lateinit var messageReceivedEvent: MessageReceivedEvent

    @Mock
    private lateinit var jda: JDA

    @Mock
    private lateinit var messageChannel: MessageChannel

    @Mock
    private lateinit var message: Message

    @Mock
    private lateinit var author: User

    @Mock
    private lateinit var messageAction: MessageAction

    @Mock
    private lateinit var injector: Injector

    @Mock
    private lateinit var definition: MockDefinition

    @Mock
    private lateinit var noParamFunction: KFunction<*>

    @Mock
    private lateinit var intIntFunction: KFunction<*>

    @Mock
    private lateinit var intStringFunction: KFunction<*>

    @Mock
    private lateinit var intStringRemainderFunction: KFunction<*>

    @Mock
    private lateinit var stringRemainderFunction: KFunction<*>

    @Mock
    private lateinit var mockIntParser: ArgumentParser<Int>

    @Mock
    private lateinit var mockStringParser: ArgumentParser<String>

    private lateinit var underTest: CommandHandler

    @Before
    fun setUp() {

        `when`(messageReceivedEvent.jda).thenReturn(jda)
        `when`(messageReceivedEvent.channel).thenReturn(messageChannel)
        `when`(messageReceivedEvent.author).thenReturn(author)
        `when`(messageReceivedEvent.message).thenReturn(message)
        `when`(author.isBot).thenReturn(false)
        `when`(messageChannel.sendMessage(ArgumentMatchers.anyString())).thenReturn(messageAction)

        `when`(mockIntParser.parseArgument("1"))
            .thenReturn(1)

        `when`(mockStringParser.parseArgument(ArgumentMatchers.anyString()))
            .thenAnswer { it.arguments.first() }

        `when`(injector.getInstance(MockDefinition::class.java)).thenReturn(definition)

        val commandMapping =
            CommandMapping(
                mapOf("test" to
                        mapOf(0 to listOf(
                            InternalCommandInfo(
                                CommandInfo(
                                    "test",
                                    listOf()),
                                MockDefinition::class,
                                noParamFunction)),
                            2 to listOf(
                                InternalCommandInfo(
                                    CommandInfo(
                                        "test",
                                        listOf(
                                            ParameterInfo("int1", intType),
                                            ParameterInfo("int2", intType))),
                                    MockDefinition::class,
                                    intIntFunction),
                                InternalCommandInfo(
                                    CommandInfo(
                                        "test",
                                        listOf(
                                            ParameterInfo("int", intType),
                                            ParameterInfo("string", stringType))),
                                    MockDefinition::class,
                                    intStringFunction)))),
                mapOf("test" to
                        mapOf(1 to listOf(
                            InternalCommandInfo(
                                CommandInfo(
                                    "test",
                                    listOf(ParameterInfo("string", stringType)),
                                    hasRemainder = true),
                                MockDefinition::class,
                                stringRemainderFunction)),
                            2 to listOf(
                                InternalCommandInfo(
                                    CommandInfo(
                                        "test",
                                        listOf(
                                            ParameterInfo("int", intType),
                                            ParameterInfo("string", stringType)),
                                        hasRemainder = true),
                                    MockDefinition::class,
                                    intStringRemainderFunction)))),
                mapOf(
                    intType to mockIntParser,
                    stringType to mockStringParser))

        underTest = CommandHandler(commandMapping, injector)
    }

    @Test
    fun noParamCommand() {
        // Given
        `when`(message.contentDisplay).thenReturn("!test")

        // When
        underTest.handleCommand(messageReceivedEvent)

        // Then
        verify(noParamFunction).call(definition)
    }

    @Test
    fun intIntCommand() {
        // Given
        `when`(message.contentDisplay).thenReturn("!test 1 1")

        // When
        underTest.handleCommand(messageReceivedEvent)

        // Then
        verify(intIntFunction).call(definition, 1, 1)
    }

    @Test
    fun intStringCommand() {
        // Given
        `when`(message.contentDisplay).thenReturn("!test 1 foo")

        // When
        underTest.handleCommand(messageReceivedEvent)

        // Then
        verify(intStringFunction).call(definition, 1, "foo")
    }

    @Test
    fun intStringRemainderCommand() {
        // Given
        `when`(message.contentDisplay).thenReturn("!test 1 foo bar star")

        // When
        underTest.handleCommand(messageReceivedEvent)

        // Then
        verify(intStringRemainderFunction).call(definition, 1, "foo bar star")
    }

    @Test
    fun stringRemainderCommand() {
        // Given
        `when`(message.contentDisplay).thenReturn("!test foo bar star")

        // When
        underTest.handleCommand(messageReceivedEvent)

        // Then
        verify(stringRemainderFunction).call(definition, "foo bar star")
    }

    @Test
    fun noCommand() {
        // Given
        `when`(message.contentDisplay).thenReturn("!othercommand")

        // When
        underTest.handleCommand(messageReceivedEvent)

        // Then
        verify(messageChannel).sendMessage("Command not found")
        verify(messageAction).queue()
    }

    @Test
    fun instantiationFailure() {
        // Given
        `when`(message.contentDisplay).thenReturn("!test")
        `when`(injector.getInstance(MockDefinition::class.java)).thenThrow(ProvisionException("Oh dear"))

        // When
        underTest.handleCommand(messageReceivedEvent)

        // Then
        verify(messageChannel).sendMessage("Failed to create instance MockDefinition")
        verify(messageAction).queue()
    }

    private open class MockDefinition : BaseCommandDefinition()
}
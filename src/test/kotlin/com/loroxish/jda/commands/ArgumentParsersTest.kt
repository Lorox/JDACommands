package com.loroxish.jda.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class UserParserTest {

    private lateinit var userParser: UserParser

    @Mock
    private lateinit var jda: JDA

    @Mock
    private lateinit var user: User

    @Before
    fun setup() {
        userParser = UserParser(jda)
    }

    @Test
    fun parseArgument() {
        // Given
        `when`(jda.getUserById(123)).thenReturn(user)

        // When
        val result = userParser.parseArgument("<@!123>")

        // Then
        assertEquals(user, result)
    }

    @Test
    fun parseArgumentNoExclamation() {
        // Given
        `when`(jda.getUserById(123)).thenReturn(user)

        // When
        val result = userParser.parseArgument("<@123>")

        // Then
        assertEquals(user, result)
    }
}

@RunWith(MockitoJUnitRunner::class)
internal class RoleParserTest {

    private lateinit var roleParser: RoleParser

    @Mock
    private lateinit var jda: JDA

    @Mock
    private lateinit var role: Role

    @Before
    fun setup() {
        roleParser = RoleParser(jda)
    }

    @Test
    fun parseArgument() {
        // Given
        `when`(jda.getRoleById(123)).thenReturn(role)

        // When
        val result = roleParser.parseArgument("<@&123>")

        // Then
        assertEquals(role, result)
    }
}

@RunWith(MockitoJUnitRunner::class)
internal class TextChannelParserTest {

    private lateinit var textChannelParser: TextChannelParser

    @Mock
    private lateinit var jda: JDA

    @Mock
    private lateinit var textChannel: TextChannel

    @Before
    fun setup() {
        textChannelParser = TextChannelParser(jda)
    }

    @Test
    fun parseArgument() {
        // Given
        `when`(jda.getTextChannelById(123)).thenReturn(textChannel)

        // When
        val result = textChannelParser.parseArgument("<@#123>")

        // Then
        assertEquals(textChannel, result)
    }
}

@RunWith(MockitoJUnitRunner::class)
internal class BooleanParserTest {

    private var booleanParser = BooleanParser()

    @Test
    fun parseArgumentTrue() {
        // When
        val result = booleanParser.parseArgument("true")

        // Then
        assertEquals(true, result)
    }

    @Test
    fun parseArgumentFalse() {
        // When
        val result = booleanParser.parseArgument("false")

        // Then
        assertEquals(false, result)
    }

    @Test
    fun parseArgumentCaps() {
        // When
        val result = booleanParser.parseArgument("TRUE")

        // Then
        assertEquals(true, result)
    }

    @Test
    fun parseArgumentOne() {
        // When
        val result = booleanParser.parseArgument("1")

        // Then
        assertEquals(true, result)
    }

    @Test
    fun parseArgumentZero() {
        // When
        val result = booleanParser.parseArgument("0")

        // Then
        assertEquals(false, result)
    }

    @Test
    fun parseArgumentNull() {
        // When
        val result = booleanParser.parseArgument("foo")

        // Then
        assertEquals(null, result)
    }
}
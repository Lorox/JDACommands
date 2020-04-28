package com.loroxish.jda.commands

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Prefix(val prefix: String)

@Target(AnnotationTarget.FUNCTION)
annotation class Command(val name: String)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class Summary(val summary: String)

@Target(AnnotationTarget.FUNCTION)
annotation class Remarks(val remarks: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Remainder

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Precondition(val evaluators: Array<KClass<out PreconditionEvaluator>>)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class Commands
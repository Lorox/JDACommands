package com.loroxish.jda.commands

@Target(AnnotationTarget.FUNCTION)
annotation class Command(val name: String)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class Summary(val summary: String)

@Target(AnnotationTarget.FUNCTION)
annotation class Remarks(val remarks: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Remainder

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class Commands
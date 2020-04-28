package com.loroxish.jda.commands

interface PreconditionEvaluator {
    fun checkCanExecute(context: CommandContext, commandInfo: CommandInfo): EvaluationResult
}

sealed class EvaluationResult {
    object Success : EvaluationResult()
    data class Failure(val message: String) : EvaluationResult()
}


package com.testy.plugin

enum class ValidationSeverity {
    ERROR,
    WARNING,
    INFO
}

data class ValidationError(
    val severity: ValidationSeverity,
    val pointer: String,
    val message: String,
    val offset: Int? = null
)


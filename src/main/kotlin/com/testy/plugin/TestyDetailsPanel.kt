package com.testy.plugin

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import javax.swing.*
import javax.swing.border.TitledBorder
import java.awt.BorderLayout
import java.awt.Font

class TestyDetailsPanel : JPanel() {
    private val contentArea = JBTextArea().apply {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        lineWrap = true
        wrapStyleWord = true
    }
    
    private val scrollPane = JBScrollPane(contentArea).apply {
        border = TitledBorder("Details")
    }
    
    init {
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)
        showEmptyState()
    }
    
    fun showEmptyState() {
        contentArea.text = "Select a node in the tree to view details"
        contentArea.font = Font(Font.SANS_SERIF, Font.ITALIC, 12)
    }
    
    fun showHttpStep(step: TestyStep.HttpStep) {
        val formatter = TestyDataFormatter
        val sb = StringBuilder()
        
        sb.append("STEP: ${step.name}\n")
        sb.append("=".repeat(50)).append("\n\n")
        
        // Step modifiers
        step.condition?.let {
            sb.append("CONDITION (when): $it\n\n")
        }
        
        step.loop?.let { loop ->
            sb.append("LOOP:\n")
            sb.append("-".repeat(30)).append("\n")
            sb.append("Variable: ${loop.variable}\n")
            loop.items?.let { sb.append("Items: ${it.size} items\n") }
            loop.range?.let { sb.append("Range: ${it.from} to ${it.to} (step: ${it.step})\n") }
            sb.append("\n")
        }
        
        step.retry?.let { retry ->
            sb.append("RETRY:\n")
            sb.append("-".repeat(30)).append("\n")
            sb.append("Attempts: ${retry.attempts}\n")
            retry.backoff?.let { sb.append("Backoff: $it\n") }
            retry.initialDelay?.let { sb.append("Initial Delay: $it\n") }
            retry.maxDelay?.let { sb.append("Max Delay: $it\n") }
            retry.retryOn?.let { sb.append("Retry On: ${it.joinToString(", ")}\n") }
            sb.append("Retry On Error: ${retry.retryOnError}\n")
            sb.append("\n")
        }
        
        step.performance?.let { perf ->
            sb.append("PERFORMANCE:\n")
            sb.append("-".repeat(30)).append("\n")
            perf.maxDuration?.let { sb.append("Max Duration: $it\n") }
            perf.warnDuration?.let { sb.append("Warn Duration: $it\n") }
            sb.append("Fail On Warning: ${perf.failOnWarning}\n")
            perf.maxMemory?.let { sb.append("Max Memory: ${it}MB\n") }
            perf.minThroughput?.let { sb.append("Min Throughput: ${it} req/s\n") }
            sb.append("\n")
        }
        
        sb.append("REQUEST:\n")
        sb.append("-".repeat(30)).append("\n")
        sb.append("Method: ${step.request.method}\n")
        sb.append("Path: ${step.request.path}\n")
        
        if (!step.request.headers.isNullOrEmpty()) {
            sb.append("\nHeaders:\n")
            sb.append(formatter.formatHeaders(step.request.headers))
            sb.append("\n")
        }
        
        if (step.request.body != null) {
            sb.append("\nBody:\n")
            sb.append(formatter.formatBody(step.request.body))
            sb.append("\n")
        }
        
        step.request.bodyFile?.let {
            sb.append("\nBody File: $it\n")
        }
        
        step.request.bodyRaw?.let {
            sb.append("\nBody Raw:\n$it\n")
        }
        
        sb.append("\n").append("=".repeat(50)).append("\n\n")
        
        sb.append("RESPONSE:\n")
        sb.append("-".repeat(30)).append("\n")
        sb.append("Status: ${step.response.status}\n")
        
        if (!step.response.headers.isNullOrEmpty()) {
            sb.append("\nHeaders:\n")
            sb.append(formatter.formatHeaders(step.response.headers))
            sb.append("\n")
        }
        
        if (step.response.json != null) {
            sb.append("\nJSON:\n")
            sb.append(formatter.formatJson(step.response.json))
            sb.append("\n")
        }
        
        step.response.text?.let {
            sb.append("\nText:\n$it\n")
        }
        
        step.response.schema?.let {
            sb.append("\nSchema File: $it\n")
        }
        
        step.response.jsonSchema?.let {
            sb.append("\nJSON Schema:\n")
            sb.append(formatter.formatBody(it))
            sb.append("\n")
        }
        
        step.response.assertions?.let { assertions ->
            sb.append("\nASSERTIONS (${assertions.size}):\n")
            sb.append("-".repeat(30)).append("\n")
            assertions.forEachIndexed { index, assertion ->
                sb.append("[${index + 1}] ${assertion.path} ${assertion.operator}")
                assertion.value?.let { sb.append(" $it") }
                assertion.message?.let { sb.append("\n    Message: $it") }
                sb.append("\n")
            }
        }
        
        if (!step.dbChecks.isNullOrEmpty()) {
            sb.append("\n").append("=".repeat(50)).append("\n\n")
            sb.append("DB CHECKS: ${step.dbChecks.size}\n")
            step.dbChecks.forEachIndexed { index, check ->
                sb.append("\n[${index + 1}] Query:\n")
                sb.append(formatter.formatSql(check.query))
                sb.append("\n")
            }
        }
        
        contentArea.text = sb.toString()
        contentArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        contentArea.caretPosition = 0
    }
    
    fun showGrpcStep(step: TestyStep.GrpcStep) {
        val formatter = TestyDataFormatter
        val sb = StringBuilder()
        
        sb.append("STEP: ${step.name}\n")
        sb.append("=".repeat(50)).append("\n\n")
        
        // Step modifiers
        step.condition?.let {
            sb.append("CONDITION (when): $it\n\n")
        }
        
        step.loop?.let { loop ->
            sb.append("LOOP:\n")
            sb.append("-".repeat(30)).append("\n")
            sb.append("Variable: ${loop.variable}\n")
            loop.items?.let { sb.append("Items: ${it.size} items\n") }
            loop.range?.let { sb.append("Range: ${it.from} to ${it.to} (step: ${it.step})\n") }
            sb.append("\n")
        }
        
        step.retry?.let { retry ->
            sb.append("RETRY:\n")
            sb.append("-".repeat(30)).append("\n")
            sb.append("Attempts: ${retry.attempts}\n")
            retry.backoff?.let { sb.append("Backoff: $it\n") }
            retry.initialDelay?.let { sb.append("Initial Delay: $it\n") }
            retry.maxDelay?.let { sb.append("Max Delay: $it\n") }
            retry.retryOn?.let { sb.append("Retry On: ${it.joinToString(", ")}\n") }
            sb.append("Retry On Error: ${retry.retryOnError}\n")
            sb.append("\n")
        }
        
        step.performance?.let { perf ->
            sb.append("PERFORMANCE:\n")
            sb.append("-".repeat(30)).append("\n")
            perf.maxDuration?.let { sb.append("Max Duration: $it\n") }
            perf.warnDuration?.let { sb.append("Warn Duration: $it\n") }
            sb.append("Fail On Warning: ${perf.failOnWarning}\n")
            perf.maxMemory?.let { sb.append("Max Memory: ${it}MB\n") }
            perf.minThroughput?.let { sb.append("Min Throughput: ${it} req/s\n") }
            sb.append("\n")
        }
        
        sb.append("gRPC REQUEST:\n")
        sb.append("-".repeat(30)).append("\n")
        sb.append("Service: ${step.grpcRequest.service}\n")
        sb.append("Method: ${step.grpcRequest.method}\n")
        
        if (!step.grpcRequest.metadata.isNullOrEmpty()) {
            sb.append("\nMetadata:\n")
            sb.append(formatter.formatHeaders(step.grpcRequest.metadata))
            sb.append("\n")
        }
        
        step.grpcRequest.message?.let { message ->
            sb.append("\nMessage:\n")
            sb.append(formatter.formatBody(message))
            sb.append("\n")
        }
        
        sb.append("\n").append("=".repeat(50)).append("\n\n")
        
        sb.append("gRPC RESPONSE:\n")
        sb.append("-".repeat(30)).append("\n")
        sb.append("Code: ${step.grpcResponse.code}\n")
        
        step.grpcResponse.message?.let {
            sb.append("\nMessage:\n")
            sb.append(formatter.formatJson(it))
            sb.append("\n")
        }
        
        if (!step.grpcResponse.metadata.isNullOrEmpty()) {
            sb.append("\nMetadata:\n")
            sb.append(formatter.formatHeaders(step.grpcResponse.metadata))
            sb.append("\n")
        }
        
        step.grpcResponse.assertions?.let { assertions ->
            sb.append("\nASSERTIONS (${assertions.size}):\n")
            sb.append("-".repeat(30)).append("\n")
            assertions.forEachIndexed { index, assertion ->
                sb.append("[${index + 1}] ${assertion.path} ${assertion.operator}")
                assertion.value?.let { sb.append(" $it") }
                assertion.message?.let { sb.append("\n    Message: $it") }
                sb.append("\n")
            }
        }
        
        if (!step.dbChecks.isNullOrEmpty()) {
            sb.append("\n").append("=".repeat(50)).append("\n\n")
            sb.append("DB CHECKS: ${step.dbChecks.size}\n")
            step.dbChecks.forEachIndexed { index, check ->
                sb.append("\n[${index + 1}] Query:\n")
                sb.append(formatter.formatSql(check.query))
                sb.append("\n")
            }
        }
        
        contentArea.text = sb.toString()
        contentArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        contentArea.caretPosition = 0
    }
    
    fun showDbCheck(check: DbCheck, index: Int) {
        val formatter = TestyDataFormatter
        val sb = StringBuilder()
        
        sb.append("DB CHECK #${index + 1}\n")
        sb.append("=".repeat(50)).append("\n\n")
        
        sb.append("QUERY:\n")
        sb.append("-".repeat(30)).append("\n")
        sb.append(formatter.formatSql(check.query))
        sb.append("\n\n")
        
        sb.append("EXPECTED RESULT:\n")
        sb.append("-".repeat(30)).append("\n")
        val resultStr = try {
            when (check.result) {
                is String -> check.result
                else -> com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(check.result)
            }
        } catch (e: Exception) {
            check.result.toString()
        }
        sb.append(formatter.formatJson(resultStr))
        
        contentArea.text = sb.toString()
        contentArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        contentArea.caretPosition = 0
    }
    
    fun showScenario(scenario: TestyScenario) {
        val sb = StringBuilder()
        
        sb.append("SCENARIO: ${scenario.name}\n")
        sb.append("=".repeat(50)).append("\n\n")
        
        if (!scenario.variables.isNullOrEmpty()) {
            sb.append("VARIABLES (${scenario.variables.size}):\n")
            sb.append("-".repeat(30)).append("\n")
            scenario.variables.forEach { (key, value) ->
                sb.append("  • $key: $value\n")
            }
            sb.append("\n")
        }
        
        if (!scenario.fixtures.isNullOrEmpty()) {
            sb.append("FIXTURES (${scenario.fixtures.size}):\n")
            sb.append("-".repeat(30)).append("\n")
            scenario.fixtures.forEach { fixture ->
                sb.append("  • $fixture\n")
            }
            sb.append("\n")
        }
        
        if (!scenario.setup.isNullOrEmpty()) {
            sb.append("SETUP HOOKS (${scenario.setup.size}):\n")
            sb.append("-".repeat(30)).append("\n")
            scenario.setup.forEachIndexed { index, hook ->
                val name = hook.name ?: "#${index + 1}"
                when {
                    hook.sql != null -> sb.append("  • [$name] SQL: ${TestyDataFormatter.truncate(hook.sql, 50)}\n")
                    hook.http != null -> sb.append("  • [$name] HTTP: ${hook.http.method} ${hook.http.path}\n")
                }
            }
            sb.append("\n")
        }
        
        if (scenario.mockServers != null && scenario.mockServers.isNotEmpty()) {
            sb.append("MOCK SERVERS (${scenario.mockServers.size}):\n")
            sb.append("-".repeat(30)).append("\n")
            scenario.mockServers.forEach { (name, server) ->
                sb.append("  • $name: ${server.routes.size} routes\n")
            }
            sb.append("\n")
        }
        
        sb.append("STEPS: ${scenario.steps.size}\n")
        sb.append("-".repeat(30)).append("\n")
        scenario.steps.forEachIndexed { index, step ->
            when (step) {
                is TestyStep.HttpStep -> {
                    sb.append("  ${index + 1}. ${step.name} - ${step.request.method} ${step.request.path} → ${step.response.status}\n")
                }
                is TestyStep.GrpcStep -> {
                    sb.append("  ${index + 1}. ${step.name} - gRPC ${step.grpcRequest.service}/${step.grpcRequest.method} → ${step.grpcResponse.code}\n")
                }
            }
        }
        
        if (!scenario.teardown.isNullOrEmpty()) {
            sb.append("\nTEARDOWN HOOKS (${scenario.teardown.size}):\n")
            sb.append("-".repeat(30)).append("\n")
            scenario.teardown.forEachIndexed { index, hook ->
                val name = hook.name ?: "#${index + 1}"
                when {
                    hook.sql != null -> sb.append("  • [$name] SQL: ${TestyDataFormatter.truncate(hook.sql, 50)}\n")
                    hook.http != null -> sb.append("  • [$name] HTTP: ${hook.http.method} ${hook.http.path}\n")
                }
            }
        }
        
        contentArea.text = sb.toString()
        contentArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        contentArea.caretPosition = 0
    }
    
    fun showHook(hook: Hook, index: Int) {
        val formatter = TestyDataFormatter
        val sb = StringBuilder()
        
        sb.append("HOOK #${index + 1}")
        hook.name?.let { sb.append(": $it") }
        sb.append("\n")
        sb.append("=".repeat(50)).append("\n\n")
        
        when {
            hook.sql != null -> {
                sb.append("TYPE: SQL\n\n")
                sb.append("QUERY:\n")
                sb.append("-".repeat(30)).append("\n")
                sb.append(formatter.formatSql(hook.sql))
            }
            hook.http != null -> {
                sb.append("TYPE: HTTP\n\n")
                sb.append("REQUEST:\n")
                sb.append("-".repeat(30)).append("\n")
                sb.append("Method: ${hook.http.method}\n")
                sb.append("Path: ${hook.http.path}\n")
                
                if (!hook.http.headers.isNullOrEmpty()) {
                    sb.append("\nHeaders:\n")
                    sb.append(formatter.formatHeaders(hook.http.headers))
                }
                
                hook.http.body?.let {
                    sb.append("\nBody:\n")
                    sb.append(formatter.formatBody(it))
                }
            }
        }
        
        contentArea.text = sb.toString()
        contentArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        contentArea.caretPosition = 0
    }
    
    fun showAssertion(assertion: Assertion, index: Int) {
        val sb = StringBuilder()
        
        sb.append("ASSERTION #${index + 1}\n")
        sb.append("=".repeat(50)).append("\n\n")
        
        sb.append("Path: ${assertion.path}\n")
        sb.append("Operator: ${assertion.operator}\n")
        assertion.value?.let {
            sb.append("Value: $it\n")
        }
        assertion.message?.let {
            sb.append("\nCustom Message: $it\n")
        }
        
        sb.append("\n")
        sb.append("-".repeat(30)).append("\n")
        sb.append("Operator Reference:\n")
        sb.append(getOperatorDescription(assertion.operator))
        
        contentArea.text = sb.toString()
        contentArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        contentArea.caretPosition = 0
    }
    
    private fun getOperatorDescription(operator: String): String {
        return when (operator.lowercase()) {
            "equals", "eq", "==" -> "Checks if values are equal"
            "notequals", "ne", "!=" -> "Checks if values are not equal"
            "greaterthan", "gt", ">" -> "Checks if actual > expected"
            "lessthan", "lt", "<" -> "Checks if actual < expected"
            "greaterorequal", "gte", ">=" -> "Checks if actual >= expected"
            "lessorequal", "lte", "<=" -> "Checks if actual <= expected"
            "between" -> "Checks if value is between [min, max]"
            "contains" -> "Checks if string/array contains value"
            "notcontains" -> "Checks if string/array does not contain value"
            "matches" -> "Checks if string matches regex pattern"
            "startswith" -> "Checks if string starts with value"
            "endswith" -> "Checks if string ends with value"
            "in" -> "Checks if value is in array"
            "notin" -> "Checks if value is not in array"
            "isempty" -> "Checks if value is empty (string/array/null)"
            "isnotempty" -> "Checks if value is not empty"
            "haslength" -> "Checks if array/string has exact length"
            "hasminlength" -> "Checks if array/string has min length"
            "hasmaxlength" -> "Checks if array/string has max length"
            else -> "Unknown operator"
        }
    }
    
    fun showError(error: ValidationError) {
        val sb = StringBuilder()
        
        sb.append("VALIDATION ERROR\n")
        sb.append("=".repeat(50)).append("\n\n")
        
        sb.append("Severity: ${error.severity}\n")
        sb.append("Message: ${error.message}\n")
        
        if (error.pointer.isNotEmpty()) {
            sb.append("Location: ${error.pointer}\n")
        }
        
        contentArea.text = sb.toString()
        contentArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        contentArea.caretPosition = 0
    }
    
    fun showMock(mockServer: String, route: MockRoute) {
        val formatter = TestyDataFormatter
        val sb = StringBuilder()
        
        sb.append("MOCK ROUTE: $mockServer\n")
        sb.append("=".repeat(50)).append("\n\n")
        
        sb.append("METHOD: ${route.method}\n")
        sb.append("PATH: ${route.path}\n\n")
        
        sb.append("RESPONSE:\n")
        sb.append("-".repeat(30)).append("\n")
        sb.append("Status: ${route.response.status}\n")
        
        if (!route.response.headers.isNullOrEmpty()) {
            sb.append("\nHeaders:\n")
            sb.append(formatter.formatHeaders(route.response.headers))
            sb.append("\n")
        }
        
        if (route.response.json != null) {
            sb.append("\nJSON:\n")
            sb.append(formatter.formatJson(route.response.json))
            sb.append("\n")
        }
        
        contentArea.text = sb.toString()
        contentArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        contentArea.caretPosition = 0
    }
}

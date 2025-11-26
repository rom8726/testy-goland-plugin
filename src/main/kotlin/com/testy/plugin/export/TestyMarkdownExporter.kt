package com.testy.plugin.export

import com.testy.plugin.*
import com.testy.plugin.TestyDataFormatter

object TestyMarkdownExporter {
    fun export(scenarios: List<TestyScenario>, fileName: String, errors: List<ValidationError>): String {
        val sb = StringBuilder()
        
        // Header
        sb.append("# Testy Scenarios: $fileName\n\n")
        
        // Statistics
        val totalSteps = scenarios.sumOf { it.steps.size }
        val totalFixtures = scenarios.sumOf { it.fixtures?.size ?: 0 }
        val totalMocks = scenarios.sumOf { it.mockServers?.size ?: 0 }
        val totalSetupHooks = scenarios.sumOf { it.setup?.size ?: 0 }
        val totalTeardownHooks = scenarios.sumOf { it.teardown?.size ?: 0 }
        val httpSteps = scenarios.sumOf { scenario -> scenario.steps.count { it is TestyStep.HttpStep } }
        val grpcSteps = scenarios.sumOf { scenario -> scenario.steps.count { it is TestyStep.GrpcStep } }
        val errorCount = errors.count { it.severity == ValidationSeverity.ERROR }
        
        sb.append("## Summary\n\n")
        sb.append("- **Scenarios**: ${scenarios.size}\n")
        sb.append("- **Steps**: $totalSteps (HTTP: $httpSteps, gRPC: $grpcSteps)\n")
        sb.append("- **Fixtures**: $totalFixtures\n")
        sb.append("- **Mock Servers**: $totalMocks\n")
        if (totalSetupHooks > 0) {
            sb.append("- **Setup Hooks**: $totalSetupHooks\n")
        }
        if (totalTeardownHooks > 0) {
            sb.append("- **Teardown Hooks**: $totalTeardownHooks\n")
        }
        if (errorCount > 0) {
            sb.append("- **Errors**: $errorCount\n")
        }
        sb.append("\n")
        
        // Scenarios
        scenarios.forEachIndexed { index, scenario ->
            sb.append("## ${index + 1}. ${scenario.name}\n\n")
            
            // Variables
            if (!scenario.variables.isNullOrEmpty()) {
                sb.append("### Variables\n\n")
                scenario.variables.forEach { (key, value) ->
                    sb.append("- `$key`: `$value`\n")
                }
                sb.append("\n")
            }
            
            // Fixtures
            if (!scenario.fixtures.isNullOrEmpty()) {
                sb.append("### Fixtures\n\n")
                scenario.fixtures.forEach { fixture ->
                    sb.append("- `$fixture`\n")
                }
                sb.append("\n")
            }
            
            // Setup Hooks
            if (!scenario.setup.isNullOrEmpty()) {
                sb.append("### Setup Hooks\n\n")
                scenario.setup.forEachIndexed { hookIndex, hook ->
                    val name = hook.name ?: "Hook ${hookIndex + 1}"
                    when {
                        hook.sql != null -> {
                            sb.append("#### $name (SQL)\n\n")
                            sb.append("```sql\n")
                            sb.append(TestyDataFormatter.formatSql(hook.sql))
                            sb.append("\n```\n\n")
                        }
                        hook.http != null -> {
                            sb.append("#### $name (HTTP)\n\n")
                            sb.append("- Method: `${hook.http.method}`\n")
                            sb.append("- Path: `${hook.http.path}`\n")
                            if (!hook.http.headers.isNullOrEmpty()) {
                                sb.append("- Headers:\n")
                                hook.http.headers.forEach { (k, v) ->
                                    sb.append("  - `$k`: `$v`\n")
                                }
                            }
                            if (hook.http.body != null) {
                                sb.append("- Body:\n")
                                sb.append("```json\n")
                                sb.append(TestyDataFormatter.formatBody(hook.http.body))
                                sb.append("\n```\n")
                            }
                            sb.append("\n")
                        }
                    }
                }
            }
            
            // Mock Servers
            if (scenario.mockServers != null && scenario.mockServers.isNotEmpty()) {
                sb.append("### Mock Servers\n\n")
                scenario.mockServers.forEach { (name, server) ->
                    sb.append("#### $name\n\n")
                    server.routes.forEach { route ->
                        sb.append("- **${route.method}** `${route.path}` → `${route.response.status}`\n")
                    }
                    sb.append("\n")
                }
            }
            
            // Steps
            sb.append("### Steps\n\n")
            scenario.steps.forEachIndexed { stepIndex, step ->
                when (step) {
                    is TestyStep.HttpStep -> exportHttpStep(sb, stepIndex, step)
                    is TestyStep.GrpcStep -> exportGrpcStep(sb, stepIndex, step)
                }
            }
            
            // Teardown Hooks
            if (!scenario.teardown.isNullOrEmpty()) {
                sb.append("### Teardown Hooks\n\n")
                scenario.teardown.forEachIndexed { hookIndex, hook ->
                    val name = hook.name ?: "Hook ${hookIndex + 1}"
                    when {
                        hook.sql != null -> {
                            sb.append("#### $name (SQL)\n\n")
                            sb.append("```sql\n")
                            sb.append(TestyDataFormatter.formatSql(hook.sql))
                            sb.append("\n```\n\n")
                        }
                        hook.http != null -> {
                            sb.append("#### $name (HTTP)\n\n")
                            sb.append("- Method: `${hook.http.method}`\n")
                            sb.append("- Path: `${hook.http.path}`\n")
                            sb.append("\n")
                        }
                    }
                }
            }
            
            sb.append("---\n\n")
        }
        
        // Errors section
        if (errors.isNotEmpty()) {
            sb.append("## Validation Errors\n\n")
            errors.forEach { error ->
                sb.append("### ${error.severity}\n\n")
                sb.append("- **Message**: ${error.message}\n")
                if (error.pointer.isNotEmpty()) {
                    sb.append("- **Location**: `${error.pointer}`\n")
                }
                sb.append("\n")
            }
        }
        
        return sb.toString()
    }
    
    private fun exportHttpStep(sb: StringBuilder, stepIndex: Int, step: TestyStep.HttpStep) {
        sb.append("#### Step ${stepIndex + 1}: ${step.name}\n\n")
        
        // Step modifiers
        step.condition?.let {
            sb.append("> **Condition**: `$it`\n\n")
        }
        
        step.loop?.let { loop ->
            sb.append("> **Loop**: var=`${loop.variable}`")
            loop.items?.let { sb.append(", items=${it.size}") }
            loop.range?.let { sb.append(", range=${it.from}..${it.to}") }
            sb.append("\n\n")
        }
        
        step.retry?.let { retry ->
            sb.append("> **Retry**: ${retry.attempts} attempts")
            retry.backoff?.let { sb.append(", $it backoff") }
            sb.append("\n\n")
        }
        
        step.performance?.let { perf ->
            sb.append("> **Performance**:")
            perf.maxDuration?.let { sb.append(" maxDuration=$it") }
            perf.warnDuration?.let { sb.append(" warnDuration=$it") }
            sb.append("\n\n")
        }
        
        sb.append("**Request:**\n")
        sb.append("- Method: `${step.request.method}`\n")
        sb.append("- Path: `${step.request.path}`\n")
        
        if (!step.request.headers.isNullOrEmpty()) {
            sb.append("- Headers:\n")
            step.request.headers.forEach { (key, value) ->
                sb.append("  - `$key`: `$value`\n")
            }
        }
        
        if (step.request.body != null) {
            sb.append("- Body:\n")
            sb.append("```json\n")
            sb.append(TestyDataFormatter.formatBody(step.request.body))
            sb.append("\n```\n")
        }
        
        step.request.bodyFile?.let {
            sb.append("- Body File: `$it`\n")
        }
        
        step.request.bodyRaw?.let {
            sb.append("- Body Raw:\n```\n$it\n```\n")
        }
        
        sb.append("\n**Response:**\n")
        sb.append("- Status: `${step.response.status}`\n")
        
        if (!step.response.headers.isNullOrEmpty()) {
            sb.append("- Headers:\n")
            step.response.headers.forEach { (key, value) ->
                sb.append("  - `$key`: `$value`\n")
            }
        }
        
        if (step.response.json != null) {
            sb.append("- JSON:\n")
            sb.append("```json\n")
            sb.append(TestyDataFormatter.formatJson(step.response.json))
            sb.append("\n```\n")
        }
        
        step.response.text?.let {
            sb.append("- Text: `$it`\n")
        }
        
        step.response.schema?.let {
            sb.append("- Schema File: `$it`\n")
        }
        
        // Assertions
        step.response.assertions?.let { assertions ->
            sb.append("\n**Assertions:**\n")
            assertions.forEachIndexed { idx, assertion ->
                sb.append("${idx + 1}. `${assertion.path}` **${assertion.operator}**")
                assertion.value?.let { sb.append(" `$it`") }
                assertion.message?.let { msg -> sb.append(" — _${msg}_") }
                sb.append("\n")
            }
        }
        
        // DB Checks
        if (!step.dbChecks.isNullOrEmpty()) {
            sb.append("\n**DB Checks:**\n")
            step.dbChecks.forEachIndexed { checkIndex, check ->
                sb.append("${checkIndex + 1}. Query:\n")
                sb.append("```sql\n")
                sb.append(TestyDataFormatter.formatSql(check.query))
                sb.append("\n```\n")
                
                sb.append("   Expected Result:\n")
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
                sb.append("```json\n")
                sb.append(TestyDataFormatter.formatJson(resultStr))
                sb.append("\n```\n")
            }
        }
        
        sb.append("\n")
    }
    
    private fun exportGrpcStep(sb: StringBuilder, stepIndex: Int, step: TestyStep.GrpcStep) {
        sb.append("#### Step ${stepIndex + 1}: ${step.name} (gRPC)\n\n")
        
        // Step modifiers
        step.condition?.let {
            sb.append("> **Condition**: `$it`\n\n")
        }
        
        step.loop?.let { loop ->
            sb.append("> **Loop**: var=`${loop.variable}`")
            loop.items?.let { sb.append(", items=${it.size}") }
            loop.range?.let { sb.append(", range=${it.from}..${it.to}") }
            sb.append("\n\n")
        }
        
        step.retry?.let { retry ->
            sb.append("> **Retry**: ${retry.attempts} attempts")
            retry.backoff?.let { sb.append(", $it backoff") }
            sb.append("\n\n")
        }
        
        step.performance?.let { perf ->
            sb.append("> **Performance**:")
            perf.maxDuration?.let { sb.append(" maxDuration=$it") }
            perf.warnDuration?.let { sb.append(" warnDuration=$it") }
            sb.append("\n\n")
        }
        
        sb.append("**gRPC Request:**\n")
        sb.append("- Service: `${step.grpcRequest.service}`\n")
        sb.append("- Method: `${step.grpcRequest.method}`\n")
        
        if (!step.grpcRequest.metadata.isNullOrEmpty()) {
            sb.append("- Metadata:\n")
            step.grpcRequest.metadata.forEach { (key, value) ->
                sb.append("  - `$key`: `$value`\n")
            }
        }
        
        step.grpcRequest.message?.let { message ->
            sb.append("- Message:\n")
            sb.append("```json\n")
            sb.append(TestyDataFormatter.formatBody(message))
            sb.append("\n```\n")
        }
        
        sb.append("\n**gRPC Response:**\n")
        sb.append("- Code: `${step.grpcResponse.code}`\n")
        
        step.grpcResponse.message?.let {
            sb.append("- Message:\n")
            sb.append("```json\n")
            sb.append(TestyDataFormatter.formatJson(it))
            sb.append("\n```\n")
        }
        
        if (!step.grpcResponse.metadata.isNullOrEmpty()) {
            sb.append("- Metadata:\n")
            step.grpcResponse.metadata.forEach { (key, value) ->
                sb.append("  - `$key`: `$value`\n")
            }
        }
        
        // Assertions
        step.grpcResponse.assertions?.let { assertions ->
            sb.append("\n**Assertions:**\n")
            assertions.forEachIndexed { idx, assertion ->
                sb.append("${idx + 1}. `${assertion.path}` **${assertion.operator}**")
                assertion.value?.let { sb.append(" `$it`") }
                assertion.message?.let { msg -> sb.append(" — _${msg}_") }
                sb.append("\n")
            }
        }
        
        // DB Checks
        if (!step.dbChecks.isNullOrEmpty()) {
            sb.append("\n**DB Checks:**\n")
            step.dbChecks.forEachIndexed { checkIndex, check ->
                sb.append("${checkIndex + 1}. Query:\n")
                sb.append("```sql\n")
                sb.append(TestyDataFormatter.formatSql(check.query))
                sb.append("\n```\n")
                
                sb.append("   Expected Result:\n")
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
                sb.append("```json\n")
                sb.append(TestyDataFormatter.formatJson(resultStr))
                sb.append("\n```\n")
            }
        }
        
        sb.append("\n")
    }
}

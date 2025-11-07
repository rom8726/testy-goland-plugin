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
        val errorCount = errors.count { it.severity == ValidationSeverity.ERROR }
        
        sb.append("## Summary\n\n")
        sb.append("- **Scenarios**: ${scenarios.size}\n")
        sb.append("- **Steps**: $totalSteps\n")
        sb.append("- **Fixtures**: $totalFixtures\n")
        sb.append("- **Mock Servers**: $totalMocks\n")
        if (errorCount > 0) {
            sb.append("- **Errors**: $errorCount\n")
        }
        sb.append("\n")
        
        // Scenarios
        scenarios.forEachIndexed { index, scenario ->
            sb.append("## ${index + 1}. ${scenario.name}\n\n")
            
            if (!scenario.fixtures.isNullOrEmpty()) {
                sb.append("### Fixtures\n\n")
                scenario.fixtures.forEach { fixture ->
                    sb.append("- `$fixture`\n")
                }
                sb.append("\n")
            }
            
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
            
            sb.append("### Steps\n\n")
            scenario.steps.forEachIndexed { stepIndex, step ->
                sb.append("#### Step ${stepIndex + 1}: ${step.name}\n\n")
                
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
}


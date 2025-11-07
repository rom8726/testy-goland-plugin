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
    
    fun showStep(step: TestyStep) {
        val formatter = TestyDataFormatter
        val sb = StringBuilder()
        
        sb.append("STEP: ${step.name}\n")
        sb.append("=".repeat(50)).append("\n\n")
        
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
    }
    
    fun showScenario(scenario: TestyScenario) {
        val sb = StringBuilder()
        
        sb.append("SCENARIO: ${scenario.name}\n")
        sb.append("=".repeat(50)).append("\n\n")
        
        if (!scenario.fixtures.isNullOrEmpty()) {
            sb.append("FIXTURES (${scenario.fixtures.size}):\n")
            sb.append("-".repeat(30)).append("\n")
            scenario.fixtures.forEach { fixture ->
                sb.append("  • $fixture\n")
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
            sb.append("  ${index + 1}. ${step.name} - ${step.request.method} ${step.request.path} → ${step.response.status}\n")
        }
        
        contentArea.text = sb.toString()
        contentArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
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
    }
}


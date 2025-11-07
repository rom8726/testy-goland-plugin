package com.testy.plugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue

class TestyCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val parent = element.parent
        
        // Complete HTTP methods
        if (isInMethodField(parent)) {
            HTTP_METHODS.forEach { method ->
                result.addElement(
                    LookupElementBuilder.create(method)
                        .withTypeText("HTTP Method")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Method)
                )
            }
        }
        
        // Complete status codes
        if (isInStatusField(parent)) {
            STATUS_CODES.forEach { status ->
                result.addElement(
                    LookupElementBuilder.create(status.toString())
                        .withTypeText("HTTP Status")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Field)
                )
            }
        }
        
        // Complete schema fields
        if (isInSchemaField(parent)) {
            SCHEMA_FIELDS.forEach { field ->
                result.addElement(
                    LookupElementBuilder.create(field)
                        .withTypeText("Schema Field")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Property)
                )
            }
        }
    }
    
    private fun isInMethodField(parent: PsiElement?): Boolean {
        return parent is YAMLKeyValue && parent.keyText == "method"
    }
    
    private fun isInStatusField(parent: PsiElement?): Boolean {
        return parent is YAMLKeyValue && parent.keyText == "status"
    }
    
    private fun isInSchemaField(parent: PsiElement?): Boolean {
        if (parent !is YAMLKeyValue) return false
        val key = parent.keyText
        return key in SCHEMA_FIELDS || key.isEmpty()
    }
    
    companion object {
        private val HTTP_METHODS = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT")
        private val STATUS_CODES = listOf(200, 201, 204, 400, 401, 403, 404, 409, 500, 502, 503)
        private val SCHEMA_FIELDS = listOf("name", "fixtures", "mockServers", "mockCalls", "steps", "request", "response", "dbChecks")
    }
}


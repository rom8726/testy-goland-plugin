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
        // Only provide completions for .testy files
        val file = parameters.originalFile
        val fileName = file.name
        if (!fileName.endsWith(".testy.yaml") && !fileName.endsWith(".testy.yml")) {
            return
        }
        
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
        
        if (isInGrpcCodeField(parent)) {
            GRPC_CODES.forEach { code ->
                result.addElement(
                    LookupElementBuilder.create(code)
                        .withTypeText("gRPC Code")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Field)
                )
            }
        }
        
        if (isInOperatorField(parent)) {
            ASSERTION_OPERATORS.forEach { (op, desc) ->
                result.addElement(
                    LookupElementBuilder.create(op)
                        .withTypeText(desc)
                        .withIcon(com.intellij.icons.AllIcons.Debugger.Watch)
                )
            }
        }
        
        if (isInBackoffField(parent)) {
            BACKOFF_STRATEGIES.forEach { strategy ->
                result.addElement(
                    LookupElementBuilder.create(strategy)
                        .withTypeText("Backoff Strategy")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Field)
                )
            }
        }
        
        if (isInSchemaField(parent)) {
            SCHEMA_FIELDS.forEach { field ->
                result.addElement(
                    LookupElementBuilder.create(field)
                        .withTypeText("Schema Field")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Property)
                )
            }
        }
        
        if (isInStepField(parent)) {
            STEP_FIELDS.forEach { field ->
                result.addElement(
                    LookupElementBuilder.create(field)
                        .withTypeText("Step Field")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Property)
                )
            }
        }
        
        if (isInRequestField(parent)) {
            REQUEST_FIELDS.forEach { field ->
                result.addElement(
                    LookupElementBuilder.create(field)
                        .withTypeText("Request Field")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Property)
                )
            }
        }
        
        if (isInResponseField(parent)) {
            RESPONSE_FIELDS.forEach { field ->
                result.addElement(
                    LookupElementBuilder.create(field)
                        .withTypeText("Response Field")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Property)
                )
            }
        }
        
        if (isInGrpcRequestField(parent)) {
            GRPC_REQUEST_FIELDS.forEach { field ->
                result.addElement(
                    LookupElementBuilder.create(field)
                        .withTypeText("gRPC Request Field")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Property)
                )
            }
        }
        
        if (isInGrpcResponseField(parent)) {
            GRPC_RESPONSE_FIELDS.forEach { field ->
                result.addElement(
                    LookupElementBuilder.create(field)
                        .withTypeText("gRPC Response Field")
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Property)
                )
            }
        }
        
        if (isInHookField(parent)) {
            HOOK_FIELDS.forEach { field ->
                result.addElement(
                    LookupElementBuilder.create(field)
                        .withTypeText("Hook Field")
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
    
    private fun isInGrpcCodeField(parent: PsiElement?): Boolean {
        return parent is YAMLKeyValue && parent.keyText == "code"
    }
    
    private fun isInOperatorField(parent: PsiElement?): Boolean {
        return parent is YAMLKeyValue && parent.keyText == "operator"
    }
    
    private fun isInBackoffField(parent: PsiElement?): Boolean {
        return parent is YAMLKeyValue && parent.keyText == "backoff"
    }
    
    private fun isInSchemaField(parent: PsiElement?): Boolean {
        if (parent !is YAMLKeyValue) return false
        val key = parent.keyText
        return key in SCHEMA_FIELDS || key.isEmpty()
    }
    
    private fun isInStepField(parent: PsiElement?): Boolean {
        if (parent !is YAMLKeyValue) return false
        val grandParent = parent.parent?.parent
        return grandParent is YAMLKeyValue && grandParent.keyText == "steps"
    }
    
    private fun isInRequestField(parent: PsiElement?): Boolean {
        if (parent !is YAMLKeyValue) return false
        val grandParent = parent.parent?.parent
        return grandParent is YAMLKeyValue && grandParent.keyText == "request"
    }
    
    private fun isInResponseField(parent: PsiElement?): Boolean {
        if (parent !is YAMLKeyValue) return false
        val grandParent = parent.parent?.parent
        return grandParent is YAMLKeyValue && grandParent.keyText == "response"
    }
    
    private fun isInGrpcRequestField(parent: PsiElement?): Boolean {
        if (parent !is YAMLKeyValue) return false
        val grandParent = parent.parent?.parent
        return grandParent is YAMLKeyValue && grandParent.keyText == "grpcRequest"
    }
    
    private fun isInGrpcResponseField(parent: PsiElement?): Boolean {
        if (parent !is YAMLKeyValue) return false
        val grandParent = parent.parent?.parent
        return grandParent is YAMLKeyValue && grandParent.keyText == "grpcResponse"
    }
    
    private fun isInHookField(parent: PsiElement?): Boolean {
        if (parent !is YAMLKeyValue) return false
        val grandParent = parent.parent?.parent?.parent?.parent
        return grandParent is YAMLKeyValue && (grandParent.keyText == "setup" || grandParent.keyText == "teardown")
    }
    
    companion object {
        private val HTTP_METHODS = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT")
        
        private val STATUS_CODES = listOf(200, 201, 204, 400, 401, 403, 404, 409, 422, 500, 502, 503, 504)
        
        private val GRPC_CODES = listOf(
            "OK", "CANCELLED", "UNKNOWN", "INVALID_ARGUMENT", "DEADLINE_EXCEEDED",
            "NOT_FOUND", "ALREADY_EXISTS", "PERMISSION_DENIED", "RESOURCE_EXHAUSTED",
            "FAILED_PRECONDITION", "ABORTED", "OUT_OF_RANGE", "UNIMPLEMENTED",
            "INTERNAL", "UNAVAILABLE", "DATA_LOSS", "UNAUTHENTICATED"
        )
        
        private val ASSERTION_OPERATORS = mapOf(
            "equals" to "Exact equality",
            "eq" to "Exact equality (alias)",
            "==" to "Exact equality (alias)",
            "notEquals" to "Not equal",
            "ne" to "Not equal (alias)",
            "!=" to "Not equal (alias)",
            "greaterThan" to "Greater than",
            "gt" to "Greater than (alias)",
            ">" to "Greater than (alias)",
            "lessThan" to "Less than",
            "lt" to "Less than (alias)",
            "<" to "Less than (alias)",
            "greaterOrEqual" to "Greater or equal",
            "gte" to "Greater or equal (alias)",
            ">=" to "Greater or equal (alias)",
            "lessOrEqual" to "Less or equal",
            "lte" to "Less or equal (alias)",
            "<=" to "Less or equal (alias)",
            "between" to "Value in range [min, max]",
            "contains" to "String/array contains",
            "notContains" to "String/array not contains",
            "matches" to "Regex match",
            "startsWith" to "String starts with",
            "endsWith" to "String ends with",
            "in" to "Value in array",
            "notIn" to "Value not in array",
            "isEmpty" to "Is empty/null",
            "isNotEmpty" to "Is not empty",
            "hasLength" to "Has exact length",
            "hasMinLength" to "Has minimum length",
            "hasMaxLength" to "Has maximum length"
        )
        
        private val BACKOFF_STRATEGIES = listOf("constant", "linear", "exponential")
        
        private val SCHEMA_FIELDS = listOf(
            "name", "variables", "fixtures", "setup", "teardown", 
            "mockServers", "mockCalls", "steps"
        )
        
        private val STEP_FIELDS = listOf(
            "name", "when", "loop", "retry", "performance",
            "request", "response", "grpcRequest", "grpcResponse", "dbChecks"
        )
        
        private val REQUEST_FIELDS = listOf(
            "method", "path", "headers", "body", "bodyFile", "bodyRaw"
        )
        
        private val RESPONSE_FIELDS = listOf(
            "status", "headers", "json", "text", "schema", "jsonSchema", "assertions"
        )
        
        private val GRPC_REQUEST_FIELDS = listOf(
            "service", "method", "message", "metadata"
        )
        
        private val GRPC_RESPONSE_FIELDS = listOf(
            "code", "message", "metadata", "assertions"
        )
        
        private val HOOK_FIELDS = listOf(
            "name", "sql", "http"
        )
    }
}
